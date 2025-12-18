package com.proyecto.StoreCollection.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private boolean activo = true;
    private Integer tiendaId;
}