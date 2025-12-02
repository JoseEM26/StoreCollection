package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.AtributoValorService;
import com.proyecto.StoreCollection.service.CarritoService;
import com.proyecto.StoreCollection.service.TiendaService;
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
public class TiendaController {

    private final TiendaService service;

    // PÚBLICO - Info de tienda por slug
    @GetMapping("/api/public/tiendas/{slug}")
    public ResponseEntity<TiendaResponse> publicInfo(@PathVariable String slug) {
        return ResponseEntity.ok(service.findBySlug(slug));
    }

    // PRIVADO - Mis tiendas
    @GetMapping("/api/owner/tiendas")
    public ResponseEntity<List<TiendaResponse>> misTiendas() {
        return ResponseEntity.ok(service.getMisTiendas());
    }

    @GetMapping("/api/owner/tiendas/mi-tienda")
    public ResponseEntity<TiendaResponse> miTienda() {
        return ResponseEntity.ok(service.getMiTienda());
    }

    @PostMapping("/api/owner/tiendas")
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/tiendas/{id}")
    public ResponseEntity<TiendaResponse> actualizar(@PathVariable Integer id,
                                                     @Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/tiendas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}