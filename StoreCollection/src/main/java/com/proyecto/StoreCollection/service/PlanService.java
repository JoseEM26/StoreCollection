package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanService {
    Page<PlanResponse> findAll(Pageable pageable);
    PlanResponse findById(Long id);
    PlanResponse save(PlanRequest request);
    PlanResponse save(PlanRequest request, Long id);
    void deleteById(Long id);
}