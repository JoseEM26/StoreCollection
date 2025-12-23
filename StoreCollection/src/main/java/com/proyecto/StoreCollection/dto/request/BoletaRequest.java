package com.proyecto.StoreCollection.dto.request;

import lombok.Data;

@Data
public class BoletaRequest {
    private String sessionId;
    private Integer userId;  // Opcional, si logueado
    private Integer tiendaId;  // Requerido para multi-tenant
}