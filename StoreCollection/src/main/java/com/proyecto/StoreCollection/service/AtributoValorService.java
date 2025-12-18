package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AtributoValorService {
    Page<AtributoValorResponse> findAll(Pageable pageable);
    List<DropDownStandard> getValoresForDropdown();
    AtributoValorResponse findById(Integer id);
    AtributoValorResponse save(AtributoValorRequest request);
    AtributoValorResponse save(AtributoValorRequest request, Integer id);
    void deleteById(Integer id);
    List<AtributoValorResponse> findByAtributoId(Integer atributoId);
    List<AtributoValorResponse> findByAtributoIdAndTiendaSlug(Integer atributoId, String tiendaSlug);
}