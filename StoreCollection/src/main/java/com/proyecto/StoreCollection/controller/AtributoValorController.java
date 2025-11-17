package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.AtributoValorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/atributos-valores")
public class AtributoValorController {

    @Autowired
    private AtributoValorService service;

    @GetMapping
    public ResponseEntity<PageResponse<AtributoValorResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AtributoValorResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtributoValorResponse> porId(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<AtributoValorResponse> crear(@Valid @RequestBody AtributoValorRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtributoValorResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtributoValorRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/atributo/{atributoId}")
    public ResponseEntity<java.util.List<AtributoValorResponse>> porAtributo(@PathVariable Long atributoId) {
        return ResponseEntity.ok(service.findByAtributoId(atributoId));
    }

    private PageResponse<AtributoValorResponse> toPageResponse(Page<AtributoValorResponse> page) {
        PageResponse<AtributoValorResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}