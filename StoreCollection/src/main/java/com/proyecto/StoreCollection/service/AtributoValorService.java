package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AtributoValorService {
    Page<AtributoValorResponse> findAll(Pageable pageable);
    AtributoValorResponse findById(Long id);
    AtributoValorResponse save(AtributoValorRequest request);
    AtributoValorResponse save(AtributoValorRequest request, Long id);
    void deleteById(Long id);
    List<AtributoValorResponse> findByAtributoId(Long atributoId);
    List<AtributoValorResponse> findByAtributoIdAndTiendaSlug(Long atributoId, String tiendaSlug);
}