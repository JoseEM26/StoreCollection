package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BoletaDetalleResponse {
    private Integer id;
    private Integer varianteId;
    private Integer cantidad;
    private BigDecimal precioUnitario;   // ← Perfecto
    private BigDecimal subtotal;         // ← Perfecto

    private String nombreProducto;
    private String sku;
    private String imagenUrl;
    private List<AtributoResponse> atributos;
}