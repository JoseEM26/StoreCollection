package com.proyecto.StoreCollection.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtributoValorResponse {
    private Integer id;
    private String valor;
    private Integer atributoId;
    private String AtributoNombre;
}