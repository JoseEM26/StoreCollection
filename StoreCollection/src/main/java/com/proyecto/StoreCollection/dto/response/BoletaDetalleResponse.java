package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BoletaDetalleResponse {
    private Integer id;
    private Integer varianteId;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}