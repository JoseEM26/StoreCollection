package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.AtributoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atributos")
public class AtributoController {

    @Autowired
    private AtributoService service;

    @GetMapping
    public ResponseEntity<PageResponse<AtributoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AtributoResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtributoResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AtributoResponse> crear(@Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtributoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtributoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<java.util.List<AtributoResponse>> porTienda(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(service.findByTiendaId(tiendaId));
    }

    private PageResponse<AtributoResponse> toPageResponse(Page<AtributoResponse> page) {
        PageResponse<AtributoResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}