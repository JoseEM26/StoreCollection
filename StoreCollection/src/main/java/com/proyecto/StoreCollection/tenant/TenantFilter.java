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

    // Detecta rutas como: /api/public/tiendas/zapatik/...
    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^/api/(public|owner)/tiendas/([^/]+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Solo procesamos GET/POST/PUT/DELETE (no OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Matcher matcher = TENANT_SLUG_PATTERN.matcher(path);
        if (matcher.find()) {
            String slug = matcher.group(2);

            Tienda tienda = tiendaRepository.findBySlug(slug)
                    .orElse(null);

            if (tienda != null) {
                TenantContext.setTenantId(tienda.getId());
            } else if (path.contains("/api/public/")) {
                response.sendError(404, "Tienda no encontrada: " + slug);
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // Siempre limpiar
        }
    }
}