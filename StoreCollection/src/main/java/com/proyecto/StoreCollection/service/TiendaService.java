package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TiendaService {
    Page<TiendaResponse> findAll(Pageable pageable);
    TiendaResponse findById(Long id);
    TiendaResponse findBySlug(String slug);
    List<TiendaResponse> findByUserId(Long userId);
    TiendaResponse save(TiendaRequest request);
    TiendaResponse save(TiendaRequest request, Long id);
    void deleteById(Long id);
}