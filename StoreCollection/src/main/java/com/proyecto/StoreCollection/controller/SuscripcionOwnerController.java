// src/main/java/com/proyecto/StoreCollection/controller/SuscripcionOwnerController.java
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.entity.TiendaSuscripcion;
import com.proyecto.StoreCollection.service.TiendaService;
import com.proyecto.StoreCollection.service.TiendaSuscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "Suscripciones - Owner", description = "Gestión de suscripción propia por el dueño de la tienda")
@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class SuscripcionOwnerController {

    private final TiendaSuscripcionService suscripcionService;
    private final TiendaService tiendaService;

    @Operation(summary = "Obtener mi suscripción actual")
    @GetMapping("/suscripcion")
    public ResponseEntity<TiendaSuscripcion> miSuscripcion() {
        Integer tiendaId = tiendaService.getTiendaDelUsuarioActual().getId();
        Optional<TiendaSuscripcion> sus = suscripcionService.obtenerSuscripcionVigente(tiendaId);
        return sus.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Cancelar mi suscripción (al final del período)")
    @PostMapping("/suscripcion/cancelar")
    public ResponseEntity<Void> cancelar(
            @RequestBody CancelRequest request) {
        Integer tiendaId = tiendaService.getTiendaDelUsuarioActual().getId();
        suscripcionService.cancelarSuscripcion(tiendaId, false, request.getMotivo());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cancelar inmediatamente (solo si lo permites)")
    @PostMapping("/suscripcion/cancelar-inmediato")
    public ResponseEntity<Void> cancelarInmediato(
            @RequestBody CancelRequest request) {
        Integer tiendaId = tiendaService.getTiendaDelUsuarioActual().getId();
        suscripcionService.cancelarSuscripcion(tiendaId, true, request.getMotivo());
        return ResponseEntity.ok().build();
    }

    // DTO simple para motivo
    public static class CancelRequest {
        private String motivo;

        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }
}