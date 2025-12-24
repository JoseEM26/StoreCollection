// Nuevo: BoletaService interface (para admin)
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.response.BoletaResponse;

import java.util.List;

public interface BoletaService {
    List<BoletaResponse> findByTienda(Integer tiendaId);
    List<BoletaResponse> findBySessionId(String sessionId); // Para cliente
    BoletaResponse findById(Integer id);
    BoletaResponse updateEstado(Integer id, String estado);
}