package com.proyecto.StoreCollection.tenant;

import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TiendaRepository tiendaRepository;

    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^/api/(public|owner)/tiendas/([^/]+)");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ((path.equals("/api/owner/productos") || path.startsWith("/api/owner/productos/")) &&
                ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Bypass para OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Matcher matcher = TENANT_SLUG_PATTERN.matcher(path);
        boolean hasTenantSlug = matcher.find();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

        try {
            Tienda tienda = null;

            if (hasTenantSlug) {
                String slug = matcher.group(2);
                tienda = tiendaRepository.findBySlug(slug).orElse(null);

                if (tienda == null && path.startsWith("/api/public/")) {
                    sendNotFound(response, "Tienda no encontrada");
                    return;
                }

                if (tienda != null) {
                    TenantContext.setTenantId(tienda.getId());
                }

            } else if (isAuthenticated && path.startsWith("/api/owner/")) {
                String email = auth.getName();
                tienda = tiendaRepository.findFirstByUserEmail(email).orElse(null);

                if (tienda != null && tienda.getActivo()) {
                    TenantContext.setTenantId(tienda.getId());
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private void sendNotFound(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}