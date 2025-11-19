package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.AtributoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AtributoController {

    private final AtributoService service;

    // PÚBLICO (para filtros en catálogo)
    @GetMapping("/api/public/tiendas/{tiendaSlug}/atributos")
    public ResponseEntity<List<AtributoResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findByTiendaSlug(tiendaSlug));
    }

    // PRIVADO
    @GetMapping("/api/owner/atributos")
    public ResponseEntity<List<AtributoResponse>> misAtributos() {
        return ResponseEntity.ok(service.findAllByTenant());
    }

    @PostMapping("/api/owner/atributos")
    public ResponseEntity<AtributoResponse> crear(@Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/atributos/{id}")
    public ResponseEntity<AtributoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/atributos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}