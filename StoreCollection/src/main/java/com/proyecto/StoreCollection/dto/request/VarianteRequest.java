package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VarianteRequest {
    private Integer id;  // Para update, si existe

    @NotBlank
    private String sku;

    @Positive
    private BigDecimal precio;

    @PositiveOrZero
    private Integer stock = 0;

    private String imagenUrl;

    private List<AtributoValorRequest> atributos;
}