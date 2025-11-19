// src/main/java/com/proyecto/StoreCollection/service/ProductoService.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {

    // Para dueño logueado
    Page<ProductoResponse> findAll(Pageable pageable);
    List<ProductoResponse> findMisProductos();
    List<ProductoResponse> findByCategoriaId(Long categoriaId);
    ProductoResponse findById(Long id);

    // Para público (por slug de tienda)
    List<ProductoResponse> findByTiendaSlug(String tiendaSlug);
    ProductoResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug);

    ProductoResponse save(ProductoRequest request);
    ProductoResponse save(ProductoRequest request, Long id);
    void deleteById(Long id);
}