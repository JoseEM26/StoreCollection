package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.CategoriaService;
import com.proyecto.StoreCollection.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    // PÚBLICO - ESTO SE UTILIZA PARA LA PARTE PUBLICA DE EL CATALOGO DONDE TODOS PUEDEN ACCEDER
    @GetMapping("/api/public/tiendas/{tiendaSlug}/categorias")
    public ResponseEntity<List<CategoriaResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findByTiendaSlug(tiendaSlug));
    }
    // PRIVADO - ESTO SE UTILIZA PARA LA PARTE PRIVADA DONDE SE RESTRINGE SEGUN EL ROL
    // CategoriaController.java

    @GetMapping("/api/owner/categorias/admin-list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Page<CategoriaResponse>> listarCategoriasUsuarioOAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        Pageable pageable = crearPageable(page, size, sort);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<CategoriaResponse> resultado;

        if (esAdmin) {
            // ADMIN → ve todo
            resultado = (search != null && !search.isBlank())
                    ? service.buscarPorNombreContainingIgnoreCase(search.trim(), pageable)
                    : service.findAll(pageable);
        } else {
            // OWNER → solo sus categorías, PERO si no tiene tienda → página vacía
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                // Usuario autenticado pero sin tienda creada → devuelve vacío
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

    @PostMapping("/api/owner/categorias")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/categorias/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/categorias/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}