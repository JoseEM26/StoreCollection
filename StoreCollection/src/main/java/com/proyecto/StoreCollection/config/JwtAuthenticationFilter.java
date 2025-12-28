package com.proyecto.StoreCollection.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;  // Este debe retornar isEnabled correctamente

    private static final Pattern[] PUBLIC_PATHS = {
            Pattern.compile("^/api/auth/(login|register)$"),
            Pattern.compile("^/api/public/.*"),
            Pattern.compile("^/swagger-ui(/.*)?$"),
            Pattern.compile("^/v3/api-docs(/.*)?$"),
            Pattern.compile("^/h2-console(/.*)?$"),
            Pattern.compile("^/favicon\\.ico$")
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        if (username == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 1. Token válido?
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    log.warn("Token inválido para usuario: {}", username);
                    sendErrorResponse(response, "Token inválido o expirado");
                    return;
                }

                // 2. Usuario activo? (esto depende de que tu UserDetailsService retorne isEnabled = activo)
                if (!userDetails.isEnabled()) {
                    log.warn("Intento de acceso con usuario inactivo: {}", username);
                    sendErrorResponse(response, "Cuenta desactivada/inactiva. Contacta al administrador.");
                    return;
                }

                // Todo OK
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Usuario autenticado con JWT: {}", username);

            } catch (Exception e) {
                log.error("Error procesando JWT para {}: {}", username, e.getMessage());
                sendErrorResponse(response, "Error de autenticación: " + e.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = String.format("{\"error\": \"%s\", \"timestamp\": \"%s\"}",
                message, java.time.Instant.now().toString());
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    private boolean isPublicPath(String uri) {
        for (Pattern pattern : PUBLIC_PATHS) {
            if (pattern.matcher(uri).matches()) {
                return true;
            }
        }
        return false;
    }
}