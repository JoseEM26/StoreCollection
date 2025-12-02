package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductoRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String slug;
    private Integer categoriaId;
    private Integer tiendaId;
}