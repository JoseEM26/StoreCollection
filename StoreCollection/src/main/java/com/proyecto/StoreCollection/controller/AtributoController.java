package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.AtributoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AtributoController {

    private final AtributoService service;

    // ========================================
    // PÚBLICO - Para filtros en catálogo
    // ========================================
    @GetMapping("/api/public/tiendas/{tiendaSlug}/atributos")
    public ResponseEntity<List<AtributoResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findByTiendaSlug(tiendaSlug));
    }

    // ========================================
    // OWNER - Gestiona solo sus atributos
    // ========================================
    @GetMapping("/api/owner/atributos")
    public ResponseEntity<List<AtributoResponse>> misAtributos() {
        return ResponseEntity.ok(service.findAllByTenant());
    }

    @PostMapping("/api/owner/atributos")
    public ResponseEntity<AtributoResponse> crear(@Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, null));
    }

    @PutMapping("/api/owner/atributos/{id}")
    public ResponseEntity<AtributoResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/atributos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================
    // ADMIN - Gestiona atributos de todas las tiendas
    // ========================================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/atributos")
    public ResponseEntity<PageResponse<AtributoResponse>> listarTodosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Integer tiendaId) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AtributoResponse> resultado = service.findAll(pageable, tiendaId);

        return ResponseEntity.ok(new PageResponse<>(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/atributos")
    public ResponseEntity<AtributoResponse> crearComoAdmin(@Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/atributos/{id}")
    public ResponseEntity<AtributoResponse> actualizarComoAdmin(
            @PathVariable Integer id,
            @Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/atributos/{id}")
    public ResponseEntity<Void> eliminarComoAdmin(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}