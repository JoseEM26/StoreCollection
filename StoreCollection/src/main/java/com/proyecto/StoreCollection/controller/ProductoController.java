// src/main/java/com/proyecto/StoreCollection/controller/ProductoController.java
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.service.ProductoService;
import com.proyecto.StoreCollection.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    //PUBLIC PARA LA PAGINA WEB DONDE TODO ES PUBLICO
    @GetMapping("/api/public/tiendas/{tiendaSlug}/productos")
    public ResponseEntity<List<ProductoCardResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findAllForPublicCatalog(tiendaSlug));
    }
    @GetMapping("/api/public/tiendas/{tiendaSlug}/productos/{productoSlug}")
    public ResponseEntity<ProductoCardResponse> publicDetail(
            @PathVariable String tiendaSlug,
            @PathVariable String productoSlug) {
        return ResponseEntity.ok(service.findByTiendaSlugAndProductoSlug(tiendaSlug, productoSlug));
    }
    //PRIVADO PARA LA PARTE ADMIN

    @GetMapping("/api/owner/productos/admin-list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Page<ProductoResponse>> listarProductosUsuarioOAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        Pageable pageable = crearPageable(page, size, sort);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<ProductoResponse> resultado;

        if (esAdmin) {
            // ADMIN → ve TODOS los productos del sistema
            resultado = (search != null && !search.isBlank())
                    ? service.buscarPorNombreContainingIgnoreCase(search.trim(), pageable)
                    : service.findAll(pageable);
        } else {
            // OWNER → solo sus productos (tenant actual)
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                // Usuario nuevo sin tienda → devuelve vacío (nunca error)
                resultado = Page.empty(pageable);
            } else {
                resultado = (search != null && !search.isBlank())
                        ? service.buscarPorNombreYEmailUsuario(search.trim(), auth.getName(), pageable)
                        : service.findByUserEmail(auth.getName(), pageable);
            }
        }

        return ResponseEntity.ok(resultado);
    }
    private Pageable crearPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String property = parts[0];
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
    @GetMapping("/api/owner/productos/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoResponse>> porCategoria(@PathVariable Integer categoriaId) {
        return ResponseEntity.ok(service.findByCategoriaId(categoriaId));
    }

    @GetMapping("/api/owner/productos/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> obtenerParaEdicion(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getProductoByIdParaEdicion(id));
    }

    @PostMapping("/api/owner/productos")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.save(request));
    }
    @PatchMapping("/api/owner/productos/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(service.toggleActivo(id));
    }
    @PutMapping("/api/owner/productos/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/productos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PageResponse<ProductoResponse> toPageResponse(Page<ProductoResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

}