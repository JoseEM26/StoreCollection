package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlanRequest {
    @NotBlank
    private String nombre;
    @PositiveOrZero
    private BigDecimal precio = BigDecimal.ZERO;
    @Positive
    private Integer maxProductos = 100;
    @Min(1) @Max(12)
    private Integer mesInicio;
    @Min(1) @Max(12)
    private Integer mesFin;
    private Boolean activo = true;  // Por defecto activo

}