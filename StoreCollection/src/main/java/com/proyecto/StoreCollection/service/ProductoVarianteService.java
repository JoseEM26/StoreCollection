package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoVarianteRequest;
import com.proyecto.StoreCollection.dto.response.ProductoVarianteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoVarianteService {
    Page<ProductoVarianteResponse> findAll(Pageable pageable);
    List<ProductoVarianteResponse> findByProductoId(Long productoId);
    ProductoVarianteResponse findById(Long id);
    ProductoVarianteResponse save(ProductoVarianteRequest request);
    ProductoVarianteResponse save(ProductoVarianteRequest request, Long id);
    void deleteById(Long id);
}