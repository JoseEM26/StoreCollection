package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/boleta")
public class BoletaController {

    // @Autowired BoletaService service;  // Implementa similar a CarritoService

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<BoletaResponse>> getBySession(@PathVariable String sessionId) {
        // return ResponseEntity.ok(service.findBySessionId(sessionId));
        return null;  // Implementa
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoletaResponse> getById(@PathVariable Integer id) {
        // return ResponseEntity.ok(service.findById(id));
        return null;  // Implementa
    }
}