package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CarritoRequest {
    @NotBlank
    private String sessionId;
    private Integer varianteId;
    @Positive
    private Integer cantidad = 1;
}