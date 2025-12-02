package com.proyecto.StoreCollection.controller;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.AtributoValorService;
import com.proyecto.StoreCollection.service.CarritoService;
import com.proyecto.StoreCollection.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/planes")  // ← ruta protegida
@PreAuthorize("hasRole('ADMIN')")     // ← doble protección
public class PlanController {

    @Autowired
    private PlanService service;

    @GetMapping
    public ResponseEntity<PageResponse<PlanResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PlanResponse> pagina = service.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPageResponse(pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> porId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PlanResponse> crear(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(service.save(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PageResponse<PlanResponse> toPageResponse(Page<PlanResponse> page) {
        PageResponse<PlanResponse> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}