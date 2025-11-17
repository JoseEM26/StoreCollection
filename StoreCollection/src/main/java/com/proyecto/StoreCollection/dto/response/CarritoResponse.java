package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class CarritoResponse {
    private Long id;
    private String sessionId;
    private Long varianteId;
    private Integer cantidad;
}