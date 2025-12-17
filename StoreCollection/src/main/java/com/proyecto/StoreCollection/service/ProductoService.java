// src/main/java/com/proyecto/StoreCollection/service/ProductoService.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponse> findAll(Pageable pageable);
    List<ProductoResponse> findByCategoriaId(Integer categoriaId);
    ProductoResponse findById(Integer id);
    List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug) ;    // Para público (por slug de tienda)
    List<DropDownStandard> getProductosForDropdown();
    ProductoCardResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug);
    ProductoResponse save(ProductoRequest request);
    ProductoResponse getProductoByIdParaEdicion(Integer id);
    ProductoResponse save(ProductoRequest request, Integer id);
    void deleteById(Integer id);
    Page<ProductoResponse> findByUserEmail(String email, Pageable pageable);
    Page<ProductoResponse> buscarPorNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<ProductoResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable);
}