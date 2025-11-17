package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AtributoService {
    Page<AtributoResponse> findAll(Pageable pageable);
    AtributoResponse findById(Long id);
    AtributoResponse save(AtributoRequest request);
    AtributoResponse save(AtributoRequest request, Long id);
    void deleteById(Long id);
    List<AtributoResponse> findByTiendaId(Long tiendaId);
}