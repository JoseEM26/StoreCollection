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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductoVarianteController {

    private final ProductoVarianteService service;

    // PÚBLICO: para detalle de producto en catálogo
    @GetMapping("/api/public/tiendas/{tiendaSlug}/productos/{productoSlug}/variantes")
    public ResponseEntity<List<ProductoVarianteResponse>> publicList(
            @PathVariable String tiendaSlug,
            @PathVariable String productoSlug) {
        return ResponseEntity.ok(service.findByProductoSlug(tiendaSlug, productoSlug));
    }

    // PRIVADO: panel del dueño
    @GetMapping("/api/owner/variantes/producto/{productoId}")
    public ResponseEntity<List<ProductoVarianteResponse>> porProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.findByProductoId(productoId)); // ya validado por tenant
    }

    @PostMapping("/api/owner/variantes")
    public ResponseEntity<ProductoVarianteResponse> crear(@Valid @RequestBody ProductoVarianteRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/variantes/{id}")
    public ResponseEntity<ProductoVarianteResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProductoVarianteRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/variantes/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}