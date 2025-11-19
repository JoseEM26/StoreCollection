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
    @RequestMapping("/api/public/carrito")
    public class CarritoController {

        private final CarritoService service;

        @GetMapping("/session/{sessionId}")
        public ResponseEntity<List<CarritoResponse>> getBySession(@PathVariable String sessionId) {
            return ResponseEntity.ok(service.findBySessionId(sessionId));
        }

        @PostMapping
        public ResponseEntity<CarritoResponse> agregar(@RequestBody CarritoRequest request) {
            return ResponseEntity.ok(service.save(request));
        }

        @PutMapping("/{id}")
        public ResponseEntity<CarritoResponse> actualizar(@PathVariable Long id,
                                                          @RequestBody CarritoRequest request) {
            return ResponseEntity.ok(service.save(request, id));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> eliminarItem(@PathVariable Long id) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/session/{sessionId}")
        public ResponseEntity<Void> vaciar(@PathVariable String sessionId) {
            service.limpiarCarrito(sessionId);
            return ResponseEntity.noContent().build();
        }
    }