package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanService {
    Page<PlanResponse> findAll(Pageable pageable);
    PlanResponse findById(Integer id);
    PlanResponse save(PlanRequest request);
    PlanResponse save(PlanRequest request, Integer id);
    void deleteById(Integer id);
    PlanResponse toggleActivo(Integer id);
}