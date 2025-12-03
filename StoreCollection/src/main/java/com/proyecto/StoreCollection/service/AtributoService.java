package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AtributoService {

    List<AtributoResponse> findByTiendaSlug(String tiendaSlug);
    List<AtributoResponse> findAllByTenant();           // ← nuevo
    Page<AtributoResponse> findAll(Pageable pageable);  // ← sigue igual (solo sus atributos)
    AtributoResponse findById(Integer id);                 // ← solo si es suyo

    AtributoResponse save(AtributoRequest request);
    AtributoResponse save(AtributoRequest request, Integer id);
    void deleteById(Integer id);
}