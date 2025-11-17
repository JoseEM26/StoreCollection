package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponse> findAll(Pageable pageable);
    List<ProductoResponse> findByTiendaId(Long tiendaId);
    List<ProductoResponse> findByCategoriaId(Long categoriaId);
    ProductoResponse findById(Long id);
    ProductoResponse save(ProductoRequest request);
    ProductoResponse save(ProductoRequest request, Long id);
    void deleteById(Long id);
}