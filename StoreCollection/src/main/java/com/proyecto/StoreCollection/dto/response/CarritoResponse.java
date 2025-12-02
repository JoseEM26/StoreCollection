package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class CarritoResponse {
    private Integer id;
    private String sessionId;
    private Integer varianteId;
    private Integer cantidad;
}