package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoVarianteRequest;
import com.proyecto.StoreCollection.dto.response.ProductoVarianteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoVarianteService {

    // === PÚBLICO: catálogo ===
    List<ProductoVarianteResponse> findByTiendaSlugAndProductoSlug(
            String tiendaSlug, String productoSlug);

    // === PRIVADO: panel del dueño ===
    Page<ProductoVarianteResponse> findAll(Pageable pageable);
    List<ProductoVarianteResponse> findByProductoId(Integer productoId); // solo si el producto es suyo
    ProductoVarianteResponse findById(Integer id);

    ProductoVarianteResponse save(ProductoVarianteRequest request);
    ProductoVarianteResponse save(ProductoVarianteRequest request, Integer id);
    void deleteById(Integer id);

    List<ProductoVarianteResponse> findByProductoSlug(String tiendaSlug, String productoSlug);
}