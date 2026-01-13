package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String slug;
    private Integer categoriaId;
    private Integer tiendaId;
    private List<VarianteRequest> variantes;
}