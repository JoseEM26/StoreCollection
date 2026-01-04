package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.dto.special.ProductoAdminListDTO;  // o mueve a dto.response
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {

    // Métodos existentes
    Page<ProductoResponse> findAll(Pageable pageable);
    List<ProductoResponse> findByCategoriaId(Integer categoriaId);
    ProductoResponse findById(Integer id);
    ProductoResponse toggleActivo(Integer id);
    List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug);
    ProductoCardResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug);
    List<DropTownStandar> getProductosForDropdown();
    ProductoResponse save(ProductoRequest request);
    ProductoResponse save(ProductoRequest request, Integer id);
    ProductoResponse getProductoByIdParaEdicion(Integer id);
    void deleteById(Integer id);
    Page<ProductoResponse> findByUserEmail(String email, Pageable pageable);
    Page<ProductoResponse> buscarPorNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<ProductoResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable);

    // Nuevos métodos para lista de administración
    Page<ProductoAdminListDTO> listarTodosAdminList(Pageable pageable);

    Page<ProductoAdminListDTO> buscarTodosPorNombreAdminList(String nombre, Pageable pageable);

    Page<ProductoAdminListDTO> listarPorTiendaAdminList(Integer tiendaId, Pageable pageable);

    Page<ProductoAdminListDTO> buscarPorNombreYTiendaAdminList(Integer tiendaId, String nombre, Pageable pageable);
}