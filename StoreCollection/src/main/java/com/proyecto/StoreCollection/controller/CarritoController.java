// CarritoController.java (modificado, agrega endpoint para checkout)
package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.Exceptions.MissingEmailConfigException;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.dto.special.ApiErrorResponse;
import com.proyecto.StoreCollection.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/carrito")
public class CarritoController {

    private final CarritoService service;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<CarritoResponse>> getBySession(
            @PathVariable String sessionId,
            @RequestParam(required = true) Integer tiendaId) {
        return ResponseEntity.ok(service.findBySessionId(sessionId, tiendaId));
    }

    @PostMapping
    public ResponseEntity<CarritoResponse> agregar(@RequestBody @Valid CarritoRequest request) {
        return ResponseEntity.ok(service.crear(request));  // ← ahora usa crear()
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarritoResponse> actualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CarritoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));  // ← ahora usa actualizar()
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarItem(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> vaciar(
            @PathVariable String sessionId,
            @RequestParam(required = true) Integer tiendaId) {
        service.limpiarCarrito(sessionId, tiendaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout/online")
    public ResponseEntity<BoletaResponse> checkoutOnline(@RequestBody @Valid BoletaRequest request) {
        return ResponseEntity.ok(service.checkoutOnline(request));
    }

    @PostMapping("/checkout/whatsapp")
    public ResponseEntity<String> checkoutWhatsapp(@RequestBody @Valid BoletaRequest request) {
        return ResponseEntity.ok(service.checkoutWhatsapp(request));
    }
}