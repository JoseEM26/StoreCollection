// CarritoService interface: Elimina checkout viejo, agrega nuevos métodos
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;

import java.util.List;

public interface CarritoService {
    List<CarritoResponse> findBySessionId(String sessionId);
    CarritoResponse findById(Integer id);
    CarritoResponse save(CarritoRequest request);
    CarritoResponse save(CarritoRequest request, Integer id);
    void deleteById(Integer id);
    void limpiarCarrito(String sessionId);

    // Nuevos
    BoletaResponse checkoutOnline(BoletaRequest request);
    String checkoutWhatsapp(BoletaRequest request);
}