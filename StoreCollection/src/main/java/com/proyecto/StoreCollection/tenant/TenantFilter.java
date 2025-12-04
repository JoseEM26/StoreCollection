// src/main/java/com/proyecto/StoreCollection/tenant/TenantFilter.java
package com.proyecto.StoreCollection.tenant;

import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1) // Antes del JWT filter
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TiendaRepository tiendaRepository;

    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^/api/(public|owner)/tiendas/([^/]+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Matcher matcher = TENANT_SLUG_PATTERN.matcher(path);
        boolean hasTenantSlug = matcher.find();

        if (hasTenantSlug) {
            String slug = matcher.group(2);
            Tienda tienda = tiendaRepository.findBySlug(slug).orElse(null);

            if (tienda == null || !tienda.getActivo()) {
                if (path.startsWith("/api/public/")) {
                    response.setStatus(404);
                    response.getWriter().write("{\"error\": \"Tienda no encontrada o inactiva\"}");
                    return;
                }
            } else {
                TenantContext.setTenantId(tienda.getId());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}