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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1) // Ejecutar antes que el filtro JWT
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TiendaRepository tiendaRepository;

    // Patrón para rutas que incluyen el slug: /api/(public|owner)/tiendas/{slug}
    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^/api/(public|owner)/tiendas/([^/]+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Saltar OPTIONS (preflight CORS)
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
            if (hasTenantSlug) {
                // Caso 1: URL con slug → resolver por slug (público o privado)
                String slug = matcher.group(2);
                Tienda tienda = tiendaRepository.findBySlug(slug).orElse(null);

                if (tienda == null || !tienda.getActivo()) {
                    if (path.startsWith("/api/public/")) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Tienda no encontrada o inactiva\"}");
                        return;
                    }
                    // Para rutas /owner con slug inválido → seguir sin tenant (se manejará en controller)
                } else {
                    TenantContext.setTenantId(tienda.getId());
                }
            } else if (isAuthenticated && path.startsWith("/api/owner/")) {
                // Caso 2: Ruta privada de OWNER sin slug → buscar tienda por email del usuario autenticado
                String email = auth.getName();
                Tienda tienda = tiendaRepository.findFirstByUserEmail(email).orElse(null);

                if (tienda != null && tienda.getActivo()) {
                    TenantContext.setTenantId(tienda.getId());
                }
                // Si no tiene tienda o está inactiva → tenantId queda null (controller devuelve vacío)
            }
            // Si es ruta pública sin slug → no se establece tenant (normal)

            filterChain.doFilter(request, response);
        } finally {
            // Siempre limpiar el ThreadLocal al final de la petición
            TenantContext.clear();
        }
    }
}