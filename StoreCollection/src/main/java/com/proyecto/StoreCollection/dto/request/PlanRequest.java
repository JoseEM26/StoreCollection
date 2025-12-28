package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60)
    private String nombre;

    @NotBlank(message = "El slug es obligatorio")
    @Size(max = 50)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug solo puede contener letras minúsculas, números y guiones")
    private String slug;

    @Size(max = 1000)
    private String descripcion;

    @PositiveOrZero(message = "El precio mensual no puede ser negativo")
    private BigDecimal precioMensual = BigDecimal.ZERO;

    @PositiveOrZero(message = "El precio anual no puede ser negativo")
    private BigDecimal precioAnual;

    @NotBlank
    @Pattern(regexp = "day|month|year", message = "Intervalo debe ser 'day', 'month' o 'year'")
    private String intervaloBilling = "month";

    @Min(1)
    private Integer intervaloCantidad = 1;

    @PositiveOrZero(message = "La duración en días debe ser positiva o cero")
    private Integer duracionDias;  // null = lifetime/indefinido

    @PositiveOrZero
    private Integer maxProductos = 100;

    @PositiveOrZero
    private Integer maxVariantes = 500;

    private Boolean esTrial = false;

    @Min(0)
    private Short diasTrial = 0;

    private Boolean esVisiblePublico = true;

    @Min(0)
    private Short orden = 999;

    private Boolean activo = true;
}