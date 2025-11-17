package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.ProductoVarianteRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.AtributoValorService;
import com.proyecto.StoreCollection.service.CarritoService;
import com.proyecto.StoreCollection.service.ProductoVarianteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/variantes")
public class ProductoVarianteController {

    @Autowired
    private ProductoVarianteService service;

    @GetMapping
    public ResponseEntity<PageResponse<ProductoVarianteResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductoVarianteResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoVarianteResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductoVarianteResponse> crear(@Valid @RequestBody ProductoVarianteRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoVarianteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoVarianteRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<java.util.List<ProductoVarianteResponse>> porProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.findByProductoId(productoId));
    }

    private PageResponse<ProductoVarianteResponse> toPageResponse(Page<ProductoVarianteResponse> page) {
        PageResponse<ProductoVarianteResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}