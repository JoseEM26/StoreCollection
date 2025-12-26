package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DetalleBoletaRequest {
    @NotNull
    private Integer varianteId;

    @Min(value = 1)
    private Integer cantidad;
}