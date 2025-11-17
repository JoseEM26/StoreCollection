package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
public interface CategoriaService {
    Page<CategoriaResponse> findAll(Pageable pageable);
    List<CategoriaResponse> findByTiendaId(Long tiendaId);
    CategoriaResponse findById(Long id);
    CategoriaResponse save(CategoriaRequest request);
    CategoriaResponse save(CategoriaRequest request, Long id);
    void deleteById(Long id);
}