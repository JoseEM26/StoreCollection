package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Planes", description = "Gestión de planes de suscripción")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // ────────────────────────────────────────────────────────────────
    // RUTAS PÚBLICAS (cualquiera puede ver los planes disponibles)
    // ────────────────────────────────────────────────────────────────

    @Operation(summary = "Obtener todos los planes públicos visibles (para página de precios)")
    @GetMapping("/public/planes")
    public ResponseEntity<List<PlanResponse>> obtenerPlanesPublicos() {
        return ResponseEntity.ok(planService.findPlanesPublicosVisibles());
    }

    @Operation(summary = "Obtener planes públicos paginados (para página de precios con filtros)")
    @GetMapping("/public/planes/paginado")
    public ResponseEntity<PageResponse<PlanResponse>> listarPlanesPublicosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orden,asc") String[] sort) {  // ← Usa 'orden' que sí existe

        Sort ordenamiento = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, ordenamiento);

        Page<PlanResponse> pagina = planService.findAllPublicos(pageable);
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    // ────────────────────────────────────────────────────────────────
    // RUTAS PROTEGIDAS (ADMIN)
    // ────────────────────────────────────────────────────────────────

    @Operation(summary = "Listar todos los planes (admin) - paginado")
    @GetMapping("/admin/planes")
    public ResponseEntity<PageResponse<PlanResponse>> listarTodosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PlanResponse> pagina = planService.findAll(pageable);
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Obtener un plan por ID (admin)")
    @GetMapping("/admin/planes/{id}")
    public ResponseEntity<PlanResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Crear un nuevo plan")
    @PostMapping("/admin/planes")
    public ResponseEntity<PlanResponse> crear(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.save(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Actualizar un plan existente")
    @PutMapping("/admin/planes/{id}")
    public ResponseEntity<PlanResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.save(request, id));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Eliminar un plan (solo si no tiene suscripciones activas)")
    @DeleteMapping("/admin/planes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        planService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Activar/Desactivar un plan")
    @PatchMapping("/admin/planes/{id}/toggle-activo")
    public ResponseEntity<PlanResponse> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.toggleActivo(id));
    }

    // ────────────────────────────────────────────────────────────────
    // Utilitarios privados
    // ────────────────────────────────────────────────────────────────

    private PageResponse<PlanResponse> toPageResponse(Page<PlanResponse> page) {
        PageResponse<PlanResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    private Sort parseSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0 ||
                (sortParams.length == 1 && sortParams[0].trim().isEmpty())) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = Arrays.stream(sortParams)
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(this::parseSingleSort)
                .collect(Collectors.toList());

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private Sort.Order parseSingleSort(String sortStr) {
        String[] parts = sortStr.split(",", 2);
        String property = parts[0].trim();
        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length > 1) {
            String dir = parts[1].trim().toUpperCase();
            if ("DESC".equals(dir)) {
                direction = Sort.Direction.DESC;
            }
        }

        return new Sort.Order(direction, property);
    }
}