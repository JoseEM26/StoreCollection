package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AtributoService {

    // === PÚBLICO (para filtros en catálogo) ===
    List<AtributoResponse> findByTiendaSlug(String tiendaSlug);

    // === PRIVADO (dueño logueado) ===
    List<AtributoResponse> findAllByTenant();           // ← nuevo
    Page<AtributoResponse> findAll(Pageable pageable);  // ← sigue igual (solo sus atributos)
    AtributoResponse findById(Long id);                 // ← solo si es suyo

    AtributoResponse save(AtributoRequest request);
    AtributoResponse save(AtributoRequest request, Long id);
    void deleteById(Long id);

    // ELIMINADO: findByTiendaId(Long tiendaId) → ya no se necesita
}