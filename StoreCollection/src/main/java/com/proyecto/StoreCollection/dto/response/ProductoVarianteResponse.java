package com.proyecto.StoreCollection.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductoVarianteResponse {
    private Integer id;
    private String sku;
    private BigDecimal precio;
    private Integer stock;
    private String imagenUrl;
    private Boolean activo;
    private Integer productoId;
    private Set<AtributoValorResponse> atributos;
    private String descripcion_corta;
    private BigDecimal precio_anterior;
}