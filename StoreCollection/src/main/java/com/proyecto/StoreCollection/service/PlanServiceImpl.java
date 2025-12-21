package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanServiceImpl implements PlanService {

    @Autowired
    private PlanRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<PlanResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse findById(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));
        return toResponse(plan);
    }

    @Override
    public PlanResponse save(PlanRequest request) { return save(request, null); }

    @Override
    public PlanResponse save(PlanRequest request, Integer id) {
        Plan plan = id == null ? new Plan() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));

        plan.setNombre(request.getNombre());
        plan.setPrecio(request.getPrecio());
        plan.setMaxProductos(request.getMaxProductos());
        plan.setMesInicio(request.getMesInicio());
        plan.setMesFin(request.getMesFin());

        return toResponse(repository.save(plan));
    }
    @Override
    @Transactional
    public PlanResponse toggleActivo(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado: " + id));

        plan.setActivo(!plan.getActivo());
        return toResponse(repository.save(plan));
    }
    @Override
    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Plan no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private PlanResponse toResponse(Plan p) {
        PlanResponse dto = new PlanResponse();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setPrecio(p.getPrecio());
        dto.setMaxProductos(p.getMaxProductos());
        dto.setMesInicio(p.getMesInicio());
        dto.setActivo(p.getActivo());
        dto.setMesFin(p.getMesFin());
        return dto;
    }
}