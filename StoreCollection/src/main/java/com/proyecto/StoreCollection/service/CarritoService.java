package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;

import java.util.List;

public interface CarritoService {
    List<CarritoResponse> findBySessionId(String sessionId);
    CarritoResponse findById(Long id);
    CarritoResponse save(CarritoRequest request);
    CarritoResponse save(CarritoRequest request, Long id);
    void deleteById(Long id);
    void limpiarCarrito(String sessionId);
}