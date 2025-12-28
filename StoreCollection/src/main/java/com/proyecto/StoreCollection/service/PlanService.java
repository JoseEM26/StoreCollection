package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlanService {
    // MÉTODOS ORIGINALES (mantengo compatibilidad)
    Page<PlanResponse> findAll(Pageable pageable);
    PlanResponse findById(Integer id);
    PlanResponse save(PlanRequest request);
    PlanResponse save(PlanRequest request, Integer id);
    void deleteById(Integer id);
    PlanResponse toggleActivo(Integer id);
    List<DropTownStandar> findOnlyTwoActiveForDropdown();
    Page<PlanResponse> findAllPublicos(Pageable pageable);
    List<PlanResponse> findPlanesPublicosVisibles();
}