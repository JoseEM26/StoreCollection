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
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TiendaController {

    private final TiendaService service;

    @GetMapping("/api/public/tiendas")
    public ResponseEntity<Page<TiendaResponse>> listarTodasTiendas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort,
            @RequestParam(required = false) String search) {

        org.springframework.data.domain.Sort.Direction direction = org.springframework.data.domain.Sort.Direction.ASC;
        String property = "nombre";

        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1) {
                property = parts[0].trim();
            }
            if (parts.length >= 2) {
                direction = "desc".equalsIgnoreCase(parts[1].trim())
                        ? org.springframework.data.domain.Sort.Direction.DESC
                        : org.springframework.data.domain.Sort.Direction.ASC;
            }
        }

        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(direction, property);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<TiendaResponse> resultado = search != null && !search.trim().isEmpty()
                ? service.buscarPorNombreContainingIgnoreCase(search.trim(), pageable)
                : service.findAll(pageable);

        return ResponseEntity.ok(resultado);
    }

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