// src/main/java/com/proyecto/StoreCollection/controller/ProductoController.java
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping("/api/public/tiendas/{tiendaSlug}/productos")
    public ResponseEntity<List<ProductoCardResponse>> publicList(@PathVariable String tiendaSlug) {
        return ResponseEntity.ok(service.findAllForPublicCatalog(tiendaSlug));
    }

    @GetMapping("/api/public/tiendas/{tiendaSlug}/productos/{productoSlug}")
    public ResponseEntity<ProductoResponse> publicDetail(
            @PathVariable String tiendaSlug, @PathVariable String productoSlug) {
        return ResponseEntity.ok(service.findByTiendaSlugAndProductoSlug(tiendaSlug, productoSlug));
    }

    // PRIVADO - Panel del dueño
    @GetMapping("/api/owner/productos")
    public ResponseEntity<PageResponse<ProductoResponse>> misProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductoResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/api/owner/productos/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoResponse>> porCategoria(@PathVariable Integer categoriaId) {
        return ResponseEntity.ok(service.findByCategoriaId(categoriaId));
    }

    @PostMapping("/api/owner/productos")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/productos/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/productos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PageResponse<ProductoResponse> toPageResponse(Page<ProductoResponse> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}