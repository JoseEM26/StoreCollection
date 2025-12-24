// BoletaController: Implementa completamente (para admin/cliente)
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.service.BoletaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/boleta") // Cambia a /api/admin si agregas auth
public class BoletaController {

    private final BoletaService service;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<BoletaResponse>> getBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.findBySessionId(sessionId));
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<List<BoletaResponse>> getByTienda(@PathVariable Integer tiendaId) {
        return ResponseEntity.ok(service.findByTienda(tiendaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoletaResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<BoletaResponse> updateEstado(@PathVariable Integer id,
                                                       @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        return ResponseEntity.ok(service.updateEstado(id, estado));
    }
}