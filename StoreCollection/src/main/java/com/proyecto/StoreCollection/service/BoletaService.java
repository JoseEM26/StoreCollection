package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoletaService {

    // Listado paginado - general (solo ADMIN)
    Page<BoletaResponse> findAll(Pageable pageable);

    // Listado paginado por tienda (ADMIN o OWNER de esa tienda)
    Page<BoletaResponse> findByTiendaId(Integer tiendaId, Pageable pageable);

    // Filtros avanzados
    Page<BoletaResponse> findByEstado(String estado, Pageable pageable);
    Page<BoletaResponse> findBySessionId(String sessionId, Pageable pageable);

    Page<BoletaResponse> findByTiendaIdAndEstado(Integer tiendaId, String estado, Pageable pageable);
    Page<BoletaResponse> findByTiendaIdAndSessionId(Integer tiendaId, String sessionId, Pageable pageable);

    // Obtención individual con verificación de permisos
    BoletaResponse findByIdConPermisos(Integer id);

    // Cambio de estado con lógica de negocio (deducción de stock)
    BoletaResponse actualizarEstado(Integer id, String estado);
}