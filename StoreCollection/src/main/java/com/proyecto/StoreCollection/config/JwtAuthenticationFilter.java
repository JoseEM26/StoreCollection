package com.proyecto.StoreCollection.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filtro JWT que autentica solicitudes con token Bearer.
 * Se ejecuta una vez por solicitud y configura el contexto de seguridad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // === PATRONES DE RUTAS PÚBLICAS (más preciso que startsWith) ===
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

        // 1. CORS PRE-FLIGHT: Permitir OPTIONS siempre
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. EXCLUIR RUTAS PÚBLICAS
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. OBTENER Y VALIDAR HEADER AUTHORIZATION
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Sin token → continuar sin autenticar (puede ser 401 más adelante)
            filterChain.doFilter(request, response);
            return;
        }

        // 4. EXTRAER JWT
        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        // 5. VALIDAR USUARIO Y TOKEN
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Token válido → autenticar
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado con JWT: {}", username);
                } else {
                    log.warn("Token JWT inválido o expirado para usuario: {}", username);
                }
            } catch (Exception e) {
                log.error("Error al procesar JWT para usuario {}: {}", username, e.getMessage());
                // No autenticar, pero continuar (puede devolver 401 en controlador)
            }
        }

        // 6. CONTINUAR CADENA DE FILTROS
        filterChain.doFilter(request, response);
    }

    /**
     * Verifica si la ruta es pública usando expresiones regulares.
     */
    private boolean isPublicPath(String uri) {
        for (Pattern pattern : PUBLIC_PATHS) {
            if (pattern.matcher(uri).matches()) {
                return true;
            }
        }
        return false;
    }
}