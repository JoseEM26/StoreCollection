package com.proyecto.StoreCollection.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AtributoResponse {
    private Long id;
    private String nombre;
    private Long tiendaId;
}