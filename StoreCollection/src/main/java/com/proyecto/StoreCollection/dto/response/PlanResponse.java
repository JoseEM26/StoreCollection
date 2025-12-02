package com.proyecto.StoreCollection.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlanResponse {
    private Integer id;
    private String nombre;
    private BigDecimal precio;
    private Integer maxProductos;
    private Integer mesInicio;
    private Integer mesFin;
}