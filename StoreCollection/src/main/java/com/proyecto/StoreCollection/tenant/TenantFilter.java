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
import java.time.LocalDate;
import java.time.Month;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TiendaRepository tiendaRepository;

    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^/api/(public|owner)/tiendas/([^/]+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

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
            } else if (isAuthenticated && path.startsWith("/api/owner/")) {
                String email = auth.getName();
                tienda = tiendaRepository.findFirstByUserEmail(email).orElse(null);
            }

            // Si no se encontró tienda → no establecer tenant
            if (tienda == null) {
                if (hasTenantSlug && path.startsWith("/api/public/")) {
                    sendError(response, "Tienda no encontrada");
                    return;
                }
                // Para owner sin tienda → se manejará en controller
            } else {
                // Validaciones de estado
                if (!tienda.getActivo()) {
                    sendError(response, "Tu tienda está inactiva. Contacta al administrador.");
                    return;
                }

                if (tienda.getPlan() == null) {
                    sendError(response, "Tu tienda no tiene un plan asignado.");
                    return;
                }

                if (!tienda.getPlan().getActivo()) {
                    sendError(response, "El plan asociado está inactivo.");
                    return;
                }

                // Validar vigencia por meses
                if (!isPlanVigenteEnMesActual(tienda.getPlan())) {
                    sendError(response, "Tu plan no está vigente en este mes. Renueva o cambia de plan.");
                    return;
                }

                // Todo OK → establecer tenant
                TenantContext.setTenantId(tienda.getId());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPlanVigenteEnMesActual(com.proyecto.StoreCollection.entity.Plan plan) {
        int mesActual = LocalDate.now().getMonthValue(); // 1 = enero, 12 = diciembre

        int inicio = plan.getMesInicio();
        int fin = plan.getMesFin();

        if (inicio <= fin) {
            // Rango normal: ej. Marzo (3) a Agosto (8)
            return mesActual >= inicio && mesActual <= fin;
        } else {
            // Rango que cruza fin de año: ej. Noviembre (11) a Febrero (2)
            return mesActual >= inicio || mesActual <= fin;
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) return;

        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 mejor que 404
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}