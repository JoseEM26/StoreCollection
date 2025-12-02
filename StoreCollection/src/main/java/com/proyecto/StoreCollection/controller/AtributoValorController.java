package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.AtributoValorService;
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
public class AtributoValorController {

    private final AtributoValorService service;

    // PÚBLICO: para filtros en catálogo
    @GetMapping("/api/public/tiendas/{tiendaSlug}/atributos/{atributoId}/valores")
    public ResponseEntity<List<AtributoValorResponse>> publicPorAtributo(
            @PathVariable String tiendaSlug,
            @PathVariable Integer atributoId) {
        return ResponseEntity.ok(service.findByAtributoIdAndTiendaSlug(atributoId, tiendaSlug));
    }

    // PRIVADO: panel del dueño
    @GetMapping("/api/owner/atributos/{atributoId}/valores")
    public ResponseEntity<List<AtributoValorResponse>> porAtributo(@PathVariable Integer atributoId) {
        return ResponseEntity.ok(service.findByAtributoId(atributoId)); // ya validado por tenant
    }

    @PostMapping("/api/owner/atributos-valores")
    public ResponseEntity<AtributoValorResponse> crear(@Valid @RequestBody AtributoValorRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/api/owner/atributos-valores/{id}")
    public ResponseEntity<AtributoValorResponse> actualizar(
            @PathVariable Integer id, @Valid @RequestBody AtributoValorRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/api/owner/atributos-valores/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}