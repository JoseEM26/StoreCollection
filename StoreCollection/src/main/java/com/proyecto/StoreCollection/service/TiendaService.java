package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.dto.special.TiendaDropdown;
import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TiendaService {
    Page<TiendaResponse> findAll(Pageable pageable);
    List<TiendaDropdown> findAllDopTownList();
    TiendaResponse findById(Integer id);
    TiendaResponse findBySlug(String slug);
    List<TiendaResponse> findByUserId(Integer userId);
    Tienda getTiendaDelUsuarioActual();           // ← clave para crear productos, etc.
    TiendaResponse getMiTienda();                 // ← para el dashboard del dueño
    Optional<Tienda> getTiendaById(Integer id);
    List<TiendaResponse> getMisTiendas();         // ← si permite varias
    Page<TiendaResponse> buscarPorNombreContainingIgnoreCase(String texto, Pageable pageable);
    TiendaResponse save(TiendaRequest request);
    TiendaResponse save(TiendaRequest request, Integer id);
    void deleteById(Integer id);
    Page<TiendaResponse> findByUserEmail(String email, Pageable pageable);
}