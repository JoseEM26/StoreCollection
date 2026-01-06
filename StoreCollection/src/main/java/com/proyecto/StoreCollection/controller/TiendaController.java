package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.service.TiendaService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TiendaController {

    private final TiendaService service;
    private final TiendaRepository tiendaRepository;

    @GetMapping("/api/public/tiendas")
    public ResponseEntity<Page<TiendaResponse>> listarTodasTiendas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        // Parsear el parámetro sort
        Sort.Direction direction = Sort.Direction.ASC;
        String property = "nombre";

        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1) {
                property = parts[0].trim();
            }
            if (parts.length >= 2 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            }
        }

        Sort sortObj = Sort.by(direction, property);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<TiendaResponse> resultado;

        if (search != null && !search.trim().isEmpty()) {
            String terminoBusqueda = search.trim().toLowerCase();

            // 1. Obtener TODAS las tiendas públicas vigentes (sin paginación)
            List<TiendaResponse> todasVigentes = service.findAllPublicasActivas(Pageable.unpaged())
                    .getContent();

            // 2. Filtrar por nombre en TODAS las tiendas
            List<TiendaResponse> coincidencias = todasVigentes.stream()
                    .filter(t -> t.getNombre().toLowerCase().contains(terminoBusqueda))
                    .collect(Collectors.toList());

            // 3. Ordenar según el sort solicitado
            if (direction == Sort.Direction.ASC) {
                coincidencias.sort(Comparator.comparing(TiendaResponse::getNombre, String.CASE_INSENSITIVE_ORDER));
            } else {
                coincidencias.sort(Comparator.comparing(TiendaResponse::getNombre, String.CASE_INSENSITIVE_ORDER).reversed());
            }

            // 4. Paginación manual sobre los resultados filtrados
            long totalCoincidencias = coincidencias.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), coincidencias.size());

            List<TiendaResponse> paginaFiltrada = coincidencias.subList(
                    Math.min(start, coincidencias.size()),
                    end
            );

            resultado = new PageImpl<>(paginaFiltrada, pageable, totalCoincidencias);

        } else {
            // Sin búsqueda: paginación normal
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
    public ResponseEntity<TiendaResponse> crearTienda(
            @RequestPart("data") @Valid TiendaRequest request,
            @RequestPart(value = "logoImg", required = false) MultipartFile logoImg) {

        request.setLogoImg(logoImg); // Asignamos el archivo al DTO
        return ResponseEntity.ok(service.save(request, null));
    }
    @PutMapping("/api/owner/tiendas/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> actualizarTienda(
            @PathVariable Integer id,
            @RequestPart("data") @Valid TiendaRequest request,
            @RequestPart(value = "logoImg", required = false) MultipartFile logoImg) {

        request.setLogoImg(logoImg);
        return ResponseEntity.ok(service.save(request, id));
    }
    @PatchMapping("/api/owner/tiendas/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TiendaResponse> toggleActivo(@PathVariable Integer id) {
        TiendaResponse response = service.toggleActivo(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/owner/tiendas/{tiendaId}/renovar")
    public ResponseEntity<TiendaResponse> renovarTienda(@PathVariable Integer tiendaId) {
        TiendaResponse response = service.renovarTienda(tiendaId);
        return ResponseEntity.ok(response);
    }
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================
    // ==================== OPERACIONES CRUD ====================

    // PRIVADO - Mis tiendas
    //@GetMapping("/api/owner/tiendas")
    //public ResponseEntity<List<TiendaResponse>> misTiendas() {
      //  return ResponseEntity.ok(service.getMisTiendas());
    //}

    //@GetMapping("/api/owner/tiendas/mi-tienda")
    //public ResponseEntity<TiendaResponse> miTienda() {
      //  return ResponseEntity.ok(service.getMiTienda());
    //}







}