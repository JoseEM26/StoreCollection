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
    CategoriaResponse findById(Long id);                 // ← solo si es suyo

    CategoriaResponse save(CategoriaRequest request);
    CategoriaResponse save(CategoriaRequest request, Long id);
    void deleteById(Long id);

    // ELIMINADO: findByTiendaId(Long tiendaId) → ya no se necesita nunca más
}