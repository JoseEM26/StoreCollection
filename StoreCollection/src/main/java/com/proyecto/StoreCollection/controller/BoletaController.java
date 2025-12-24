package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.service.BoletaService;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/boletas")
public class BoletaController {

    private final BoletaService service;

    @GetMapping("/admin-list")
    public ResponseEntity<Page<BoletaResponse>> listarBoletas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fecha,desc") String sort,
            @RequestParam(required = false) String estado) {

        Pageable pageable = crearPageable(page, size, sort);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ← AGREGAR ESTO (igual que en tu ejemplo de tiendas)
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Page.empty(pageable)); // O throw 401 si prefieres
        }

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<BoletaResponse> resultado;

        if (esAdmin) {
            if (estado != null && !estado.isBlank()) {
                resultado = service.findByEstado(estado.toUpperCase(), pageable);
            } else {
                resultado = service.findAll(pageable);
            }
        } else {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                resultado = Page.empty(pageable);
            } else {
                if (estado != null && !estado.isBlank()) {
                    resultado = service.findByTiendaIdAndEstado(tenantId, estado.toUpperCase(), pageable);
                } else {
                    resultado = service.findByTiendaId(tenantId, pageable);
                }
            }
        }

        return ResponseEntity.ok(resultado);
    }

    // Método auxiliar para crear Pageable
    private Pageable crearPageable(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String property = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
    // Detalle de una boleta (con verificación de permisos)
    @GetMapping("/{id}")
    public ResponseEntity<BoletaResponse> getBoletaById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findByIdConPermisos(id));
    }

    // Cambio de estado (atender / cancelar)
    @PutMapping("/{id}/estado")
    public ResponseEntity<BoletaResponse> actualizarEstado(
            @PathVariable Integer id,
            @RequestBody EstadoUpdateRequest request) {

        return ResponseEntity.ok(service.actualizarEstado(id, request.estado()));
    }

    // DTO simple para el body del PUT
    record EstadoUpdateRequest(String estado) {}

}