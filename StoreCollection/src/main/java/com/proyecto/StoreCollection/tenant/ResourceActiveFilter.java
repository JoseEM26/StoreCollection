// src/main/java/com/proyecto/StoreCollection/tenant/ResourceActiveFilter.java
package com.proyecto.StoreCollection.tenant;

import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import com.proyecto.StoreCollection.repository.ProductoRepository;
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
@Order(2)
@RequiredArgsConstructor
public class ResourceActiveFilter extends OncePerRequestFilter {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    // CORREGIDO: ruta real del detalle de producto
    private static final Pattern PRODUCTO_SLUG_PATTERN = Pattern.compile("^/api/public/tiendas/[^/]+/productos/([^/]+)");
    private static final Pattern PRODUCTO_ID_PATTERN = Pattern.compile("^/api/public/tiendas/[^/]+/productos/(\\d+)");

    // Opcional: si en el futuro tienes detalle de categoría por slug
    // private static final Pattern CATEGORIA_SLUG_PATTERN = Pattern.compile("^/api/public/tiendas/[^/]+/categorias/([^/]+)");
    private static final Pattern CATEGORIA_ID_PATTERN = Pattern.compile("^/api/public/tiendas/[^/]+/categorias/(\\d+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Integer tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }
// NUEVO: Excluir explícitamente el listado de productos
        if (path.matches("^/api/public/tiendas/[^/]+/productos$")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // Producto por slug (detalle)
            Matcher prodSlugMatcher = PRODUCTO_SLUG_PATTERN.matcher(path);
            if (prodSlugMatcher.find()) {
                String productoSlug = prodSlugMatcher.group(1);
                Producto producto = productoRepository.findBySlugAndTiendaId(productoSlug, tenantId)
                        .orElse(null);

                if (producto == null || !producto.isActivo()) {
                    sendError(response, "El producto no está disponible en este momento.");
                    return;
                }
            }

            // Producto por ID (si usas)
            Matcher prodIdMatcher = PRODUCTO_ID_PATTERN.matcher(path);
            if (prodIdMatcher.find()) {
                Integer productoId = Integer.parseInt(prodIdMatcher.group(1));
                Producto producto = productoRepository.findByIdAndTiendaId(productoId, tenantId)
                        .orElse(null);

                if (producto == null || !producto.isActivo()) {
                    sendError(response, "El producto no está disponible en este momento.");
                    return;
                }
            }

            // Categoría por ID (si aplica)
            Matcher catIdMatcher = CATEGORIA_ID_PATTERN.matcher(path);
            if (catIdMatcher.find()) {
                Integer categoriaId = Integer.parseInt(catIdMatcher.group(1));
                Categoria categoria = categoriaRepository.findByIdAndTiendaId(categoriaId, tenantId)
                        .orElse(null);

                if (categoria == null || !categoria.isActivo()) {
                    sendError(response, "La categoría no está disponible en este momento.");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error en ResourceActiveFilter", e);
            sendError(response, "Error al validar el recurso solicitado.");        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}