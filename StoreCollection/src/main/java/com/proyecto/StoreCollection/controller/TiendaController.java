package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.service.TiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TiendaController {

    private final TiendaService service;
// En tu controlador (probablemente TiendaPublicController o el que tenga el endpoint)

    @GetMapping("/api/public/tiendas")
    public ResponseEntity<Page<TiendaResponse>> listarTodasTiendas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,  // Puedes mantener 12 como en frontend
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        org.springframework.data.domain.Sort.Direction direction = org.springframework.data.domain.Sort.Direction.ASC;
        String property = "nombre";

        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1) property = parts[0].trim();
            if (parts.length >= 2) {
                direction = "desc".equalsIgnoreCase(parts[1].trim())
                        ? org.springframework.data.domain.Sort.Direction.DESC
                        : org.springframework.data.domain.Sort.Direction.ASC;
            }
        }

        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(direction, property);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<TiendaResponse> resultado;

        if (search != null && !search.trim().isEmpty()) {
            // Búsqueda solo entre tiendas activas con plan vigente
            Page<TiendaResponse> todasActivas = service.findAllPublicasActivas(pageable);
            resultado = todasActivas.getContent().stream()
                    .filter(t -> t.getNombre().toLowerCase().contains(search.trim().toLowerCase()))
                    .collect(Collectors.toList())
                    .stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> new PageImpl(list, pageable, list.size())
                    ));
        } else {
            resultado = service.findAllPublicasActivas(pageable);
        }

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/api/public/tiendas/{slug}")
    public ResponseEntity<TiendaResponse> publicInfo(@PathVariable String slug) {
        return ResponseEntity.ok(service.findBySlug(slug));
    }
    //ESTO ES LA TIENDA privada DONDE por roles ACCEDEN
    @GetMapping("/api/owner/tiendas/admin-list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Page<TiendaResponse>> listarTiendasUsuarioOAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        Pageable pageable = crearPageable(page, size, sort);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<TiendaResponse> resultado;

        if (esAdmin) {
            // ADMIN → ve todas
            resultado = search != null && !search.isBlank()
                    ? service.buscarPorNombreContainingIgnoreCase(search.trim(), pageable)
                    : service.findAll(pageable);
        } else {
            // OWNER → solo las suyas
            resultado = service.findByUserEmail(auth.getName(), pageable);
        }

        return ResponseEntity.ok(resultado);
    }
    private Pageable crearPageable(int page, int size, String sort) {
        String[] parts = sort.split(",");
        String property = parts[0];
        org.springframework.data.domain.Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;

        return PageRequest.of(page, size, org.springframework.data.domain.Sort.by(direction, property));
    }
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================

    // NUEVO: Endpoint para obtener tienda específica (para edición)
    @GetMapping("/api/owner/tiendas/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> obtenerTiendaPorId(@PathVariable Integer id) {
        // Necesitarás agregar este método en tu service
        // return ResponseEntity.ok(service.getTiendaByIdParaEdicion(id));

        // Por ahora, usa el existente (pero esto no verifica permisos)
        // Te recomiendo implementar el método en el service como mostré antes
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/api/owner/tiendas")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> crearTienda(@Valid @RequestBody TiendaRequest request) {
        // IMPORTANTE: En el service, asegúrate de validar que:
        // 1. OWNER solo pueda crear tiendas para sí mismo
        // 2. ADMIN pueda crear tiendas para cualquier usuario
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/tiendas/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> actualizarTienda(
            @PathVariable Integer id,
            @Valid @RequestBody TiendaRequest request) {

        // IMPORTANTE: En el service.save(request, id) debes:
        // 1. Verificar que el usuario autenticado es dueño de la tienda (si es OWNER)
        // 2. Permitir a ADMIN editar cualquier tienda
        // 3. No permitir cambiar el usuario propietario en edición
        return ResponseEntity.ok(service.save(request, id));
    }
    @PatchMapping("/api/owner/tiendas/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> toggleActivo(@PathVariable Integer id) {
        TiendaResponse response = service.toggleActivo(id);
        return ResponseEntity.ok(response);
    }
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================

    // PRIVADO - Mis tiendas
    @GetMapping("/api/owner/tiendas")
    public ResponseEntity<List<TiendaResponse>> misTiendas() {
        return ResponseEntity.ok(service.getMisTiendas());
    }

    @GetMapping("/api/owner/tiendas/mi-tienda")
    public ResponseEntity<TiendaResponse> miTienda() {
        return ResponseEntity.ok(service.getMiTienda());
    }






}