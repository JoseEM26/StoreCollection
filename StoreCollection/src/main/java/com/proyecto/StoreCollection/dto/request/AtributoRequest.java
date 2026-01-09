package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtributoRequest {
    @NotBlank
    private String nombre;
    private Integer tiendaId;
}