package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductoVarianteRequest {
    @NotBlank
    private String sku;
    @Positive
    private BigDecimal precio;
    @PositiveOrZero
    private Integer stock = 0;
    private String imagenUrl;
    private Boolean activo = true;
    private Integer productoId;
    private Set<Integer> atributoValorIds;
    private String descripcion_corta;
    private BigDecimal precio_anterior;
}