package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;
import com.proyecto.StoreCollection.entity.Carrito;
import com.proyecto.StoreCollection.entity.Variante;
import com.proyecto.StoreCollection.repository.CarritoRepository;
import com.proyecto.StoreCollection.repository.ProductoVarianteRepository;
import com.proyecto.StoreCollection.repository.VarianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CarritoServiceImpl implements CarritoService {

    @Autowired
    private CarritoRepository repository;

    @Autowired
    private VarianteRepository varianteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CarritoResponse> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse findById(Long id) {
        Carrito carrito = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));
        return toResponse(carrito);
    }

    @Override
    public CarritoResponse save(CarritoRequest request) { return save(request, null); }

    @Override
    public CarritoResponse save(CarritoRequest request, Long id) {
        Carrito carrito = id == null ? new Carrito() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));

        carrito.setSessionId(request.getSessionId());
        carrito.setCantidad(request.getCantidad());
        if (request.getVarianteId() != null) {
            Variante v = varianteRepository.findById(request.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada"));
            carrito.setVariante(v);
        }

        return toResponse(repository.save(carrito));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void limpiarCarrito(String sessionId) {
        repository.deleteBySessionId(sessionId);
    }

    private CarritoResponse toResponse(Carrito c) {
        CarritoResponse dto = new CarritoResponse();
        dto.setId(c.getId());
        dto.setSessionId(c.getSessionId());
        dto.setCantidad(c.getCantidad());
        if (c.getVariante() != null) {
            dto.setVarianteId(c.getVariante().getId());
        }
        return dto;
    }
}