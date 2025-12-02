package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    // PÚBLICO - para menú y filtros
    @GetMapping("/api/public/tiendas/{tiendaSlug}/categorias")
    public ResponseEntity<List<CategoriaResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findByTiendaSlug(tiendaSlug));
    }

    // PRIVADO - panel del dueño
    @GetMapping("/api/owner/categorias")
    public ResponseEntity<List<CategoriaResponse>> misCategorias() {
        return ResponseEntity.ok(service.findAllByTenant());
    }

    @PostMapping("/api/owner/categorias")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/categorias/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/categorias/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}