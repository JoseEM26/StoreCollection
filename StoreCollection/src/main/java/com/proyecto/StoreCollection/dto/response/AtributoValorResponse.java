package com.proyecto.StoreCollection.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtributoValorResponse {
    private Long id;
    private String valor;
    private Long atributoId;
    private String AtributoNombre;
}