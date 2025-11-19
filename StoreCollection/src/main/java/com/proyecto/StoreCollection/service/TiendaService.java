package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TiendaService {
    Page<TiendaResponse> findAll(Pageable pageable);
    TiendaResponse findById(Long id);
    TiendaResponse findBySlug(String slug);
    List<TiendaResponse> findByUserId(Long userId);

    // NUEVOS: para multi-tenant
    Tienda getTiendaDelUsuarioActual();           // ← clave para crear productos, etc.
    TiendaResponse getMiTienda();                 // ← para el dashboard del dueño
    List<TiendaResponse> getMisTiendas();         // ← si permite varias

    TiendaResponse save(TiendaRequest request);
    TiendaResponse save(TiendaRequest request, Long id);
    void deleteById(Long id);
}