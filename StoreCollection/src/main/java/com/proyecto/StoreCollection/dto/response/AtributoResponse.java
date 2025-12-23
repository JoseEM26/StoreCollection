package com.proyecto.StoreCollection.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtributoResponse {
    private Integer id;
    private String nombre;
    private Integer tiendaId;
    private String valor;

}