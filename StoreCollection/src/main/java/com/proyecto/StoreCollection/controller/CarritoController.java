    package com.proyecto.StoreCollection.controller;
    import com.proyecto.StoreCollection.dto.request.AtributoRequest;
    import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
    import com.proyecto.StoreCollection.dto.request.CarritoRequest;
    import com.proyecto.StoreCollection.dto.response.AtributoResponse;
    import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
    import com.proyecto.StoreCollection.dto.response.CarritoResponse;
    import com.proyecto.StoreCollection.dto.response.PageResponse;
    import com.proyecto.StoreCollection.service.AtributoService;
    import com.proyecto.StoreCollection.service.AtributoValorService;
    import com.proyecto.StoreCollection.service.CarritoService;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;
    import java.util.stream.Collectors;

    @RestController
    @RequestMapping("/api/carrito")
    public class CarritoController {

        @Autowired
        private CarritoService service;

        @GetMapping("/session/{sessionId}")
        public ResponseEntity<java.util.List<CarritoResponse>> porSession(@PathVariable String sessionId) {
            return ResponseEntity.ok(service.findBySessionId(sessionId));
        }

        @GetMapping("/{id}")
        public ResponseEntity<CarritoResponse> porId(@PathVariable Long id) {
            return ResponseEntity.ok(service.findById(id));
        }

        @PostMapping
        public ResponseEntity<CarritoResponse> agregar(@Valid @RequestBody CarritoRequest request) {
            return ResponseEntity.ok(service.save(request));
        }

        @PutMapping("/{id}")
        public ResponseEntity<CarritoResponse> actualizar(
                @PathVariable Long id,
                @Valid @RequestBody CarritoRequest request) {
            return ResponseEntity.ok(service.save(request, id));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> eliminar(@PathVariable Long id) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/session/{sessionId}")
        public ResponseEntity<Void> limpiar(@PathVariable String sessionId) {
            service.limpiarCarrito(sessionId);
            return ResponseEntity.noContent().build();
        }
    }