// CarritoService interface: Elimina checkout viejo, agrega nuevos métodos
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;

import java.util.List;

public interface CarritoService {
    List<CarritoResponse> findBySessionId(String sessionId, Integer tiendaId);
    CarritoResponse findById(Integer id);
    void deleteById(Integer id);
    void limpiarCarrito(String sessionId, Integer tiendaId);
    CarritoResponse crear(CarritoRequest request);
    CarritoResponse actualizar(Integer carritoId, CarritoRequest request);
    BoletaResponse checkoutOnline(BoletaRequest request);
    String checkoutWhatsapp(BoletaRequest request);
}