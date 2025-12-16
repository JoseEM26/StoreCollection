package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TiendaService {
    Page<TiendaResponse> findAll(Pageable pageable);
    TiendaResponse findById(Integer id);
    TiendaResponse findBySlug(String slug);
    List<TiendaResponse> findByUserId(Integer userId);
     List<Tienda> findAllActivas() ;
    Tienda getTiendaDelUsuarioActual();           // ← clave para crear productos, etc.
    TiendaResponse getMiTienda();                 // ← para el dashboard del dueño
    List<TiendaResponse> getMisTiendas();         // ← si permite varias
    Page<TiendaResponse> buscarPorNombreContainingIgnoreCase(String texto, Pageable pageable);
    TiendaResponse save(TiendaRequest request);
    TiendaResponse save(TiendaRequest request, Integer id);
    void deleteById(Integer id);
    Page<TiendaResponse> findByUserEmail(String email, Pageable pageable);
}