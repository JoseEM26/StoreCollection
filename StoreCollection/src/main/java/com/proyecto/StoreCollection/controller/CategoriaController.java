package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService service;

    @GetMapping
    public ResponseEntity<PageResponse<CategoriaResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CategoriaResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<java.util.List<CategoriaResponse>> porTienda(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(service.findByTiendaId(tiendaId));
    }

    private PageResponse<CategoriaResponse> toPageResponse(Page<CategoriaResponse> page) {
        PageResponse<CategoriaResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}