package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String slug;
    private Long tiendaId;
    private boolean activo = true;

}