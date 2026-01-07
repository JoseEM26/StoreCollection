package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.BoletaAdminRequest;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.service.BoletaService;
import com.proyecto.StoreCollection.service.PdfService;
import com.proyecto.StoreCollection.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/boletas")
public class BoletaController {
    private final BoletaService service;
    private final PdfService pdfService;
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
    @PostMapping("/crear-venta-directa")
    public ResponseEntity<?> crearBoletaAdmin(@Valid @RequestBody BoletaAdminRequest request) {
        try {
            BoletaResponse response = service.crearBoletaAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Datos inválidos al crear boleta: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos inválidos", "message", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Error de negocio: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Stock insuficiente", "message", e.getMessage()));

        } catch (AccessDeniedException e) {
            log.warn("Acceso denegado para tienda {}: {}", request.getTiendaId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado"));

        } catch (Exception e) {
            log.error("Error inesperado al crear boleta directa", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
    @GetMapping("/{id}/whatsapp-confirmacion")
    public ResponseEntity<String> generarWhatsappConfirmacionCliente(@PathVariable Integer id) {
        try {
            String whatsappUrl = service.generarMensajeConfirmacionCliente(id);
            return ResponseEntity.ok(whatsappUrl);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/factura-pdf")
    public ResponseEntity<byte[]> descargarFacturaPdf(@PathVariable Integer id) {
        BoletaResponse boleta = service.findByIdConPermisos(id);

        if (!"ATENDIDA".equals(boleta.getEstado())) {
            return ResponseEntity.notFound().build();  // 404 Not Found → más lógico
        }

        try {
            byte[] pdf = pdfService.generarFacturaPdf(boleta);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("factura_boleta_" + id + ".pdf")
                    .build());
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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