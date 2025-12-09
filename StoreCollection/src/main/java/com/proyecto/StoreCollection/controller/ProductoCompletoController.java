package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.config.AuthService;
import com.proyecto.StoreCollection.dto.request.ProductoCompletoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoCreateResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import com.proyecto.StoreCollection.service.ProductoCompletoServiceImpl;
import com.proyecto.StoreCollection.service.ProductoService;
import com.proyecto.StoreCollection.service.TiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/owner/productos")
@RequiredArgsConstructor
public class ProductoCompletoController {

    private final ProductoCompletoServiceImpl service;
    private final ProductoRepository productoRepository;
    private final AuthService authService;
    private final TiendaService tiendaService;
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



    // === OBTENER UN PRODUCTO POR ID (con seguridad multitenant) ===
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerProducto(@PathVariable Integer id) {

        Producto producto;

        // Si es ADMIN → puede ver cualquier producto
        if (authService.isAdmin()) {
            producto = productoRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        }
        // Si es OWNER → solo puede ver los de su tienda
        else {
            producto = productoRepository.getByIdAndTenant(id); // ← Ya filtra por tienda del usuario actual
        }

        // Mapeo completo con variantes y atributos
        ProductoResponse response = mapToProductoResponse(producto);
        return ResponseEntity.ok(response);
    }

    // === Mapeo reutilizable ===
    private ProductoResponse mapToProductoResponse(Producto p) {
        ProductoResponse resp = new ProductoResponse();
        resp.setId(p.getId());
        resp.setNombre(p.getNombre());
        resp.setSlug(p.getSlug());
        resp.setCategoriaId(p.getCategoria().getId());

        List<ProductoResponse.VarianteResponse> variantesResp = p.getVariantes().stream()
                .map(variante -> {
                    ProductoResponse.VarianteResponse vr = new ProductoResponse.VarianteResponse();
                    vr.setId(variante.getId());
                    vr.setSku(variante.getSku());
                    vr.setPrecio(variante.getPrecio());
                    vr.setStock(variante.getStock());
                    vr.setImagenUrl(variante.getImagenUrl());
                    vr.setActivo(variante.getActivo());

                    // Atributos con nombre y valor (Color: Rojo, Talla: M)
                    List<ProductoResponse.AtributoResponse> attrs = variante.getAtributos().stream()
                            .map(av -> new ProductoResponse.AtributoResponse(
                                    av.getAtributo().getNombre(),  // ← "Color"
                                    av.getValor()                   // ← "Rojo"
                            ))
                            .sorted(Comparator.comparing(ProductoResponse.AtributoResponse::getNombreAtributo))
                            .toList();

                    vr.setAtributos(attrs);
                    return vr;
                })
                .toList();

        resp.setVariantes(variantesResp);
        return resp;
    }
}