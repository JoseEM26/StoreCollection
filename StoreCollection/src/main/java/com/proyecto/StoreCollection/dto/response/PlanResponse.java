package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanResponse {

    private Integer id;
    private String nombre;
    private String slug;
    private String descripcion;

    private BigDecimal precioMensual;
    private BigDecimal precioAnual;

    private String intervaloBilling;
    private Integer intervaloCantidad;
    private Integer duracionDias;

    private Integer maxProductos;
    private Integer maxVariantes;

    private Boolean esTrial;
    private Short diasTrial;

    private Boolean esVisiblePublico;
    private Short orden;

    private Boolean activo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}