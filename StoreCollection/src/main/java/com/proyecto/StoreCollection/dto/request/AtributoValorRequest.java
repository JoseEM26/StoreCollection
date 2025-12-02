package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtributoValorRequest {
    @NotBlank
    private String valor;
    private Integer atributoId;
}