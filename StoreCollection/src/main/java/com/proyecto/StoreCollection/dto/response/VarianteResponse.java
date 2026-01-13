package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VarianteResponse {
    private Integer id;
    private String sku;
    private BigDecimal precio;
    private Integer stock;
    private String imagenUrl;
    private boolean activo;
    private List<AtributoValorResponse> atributos;
    private String descripcion_corta;
    private BigDecimal precio_anterior;
}