package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.request.ProductoCompletoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoCreateResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.service.ProductoCompletoServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/productos")
@RequiredArgsConstructor
public class ProductoCompletoController {

    private final ProductoCompletoServiceImpl service;

    // CREAR
    @PostMapping("/completo")
    public ResponseEntity<ProductoCreateResponse> crear(
            @Valid @RequestBody ProductoCompletoRequest request) {
        ProductoCreateResponse creado = service.crearProductoCompleto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // ACTUALIZAR
    @PutMapping("/completo/{id}")
    public ResponseEntity<ProductoCreateResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoCompletoRequest request) {
        ProductoCreateResponse actualizado = service.actualizarProductoCompleto(id, request);
        return ResponseEntity.ok(actualizado);
    }
}