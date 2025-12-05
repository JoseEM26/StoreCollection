package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoriaService {

    // === PÚBLICO: para el menú del catálogo ===
    List<CategoriaResponse> findByTiendaSlug(String tiendaSlug);

    // === PRIVADO: panel del dueño ===
    List<CategoriaResponse> findAllByTenant();           // ← nuevo
    Page<CategoriaResponse> findAll(Pageable pageable);  // ← solo sus categorías
    CategoriaResponse findById(Integer id);                 // ← solo si es suyo

    CategoriaResponse save(CategoriaRequest request);
    CategoriaResponse save(CategoriaRequest request, Integer id);
    void deleteById(Integer id);

    Page<CategoriaResponse> findByUserEmail(String email, Pageable pageable);  // ← este es el que usamos
     Page<CategoriaResponse> buscarPorNombreContainingIgnoreCase(String nombre, Pageable pageable);

     Page<CategoriaResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable);}