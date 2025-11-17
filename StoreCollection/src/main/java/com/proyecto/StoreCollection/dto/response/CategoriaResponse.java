package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private String slug;
    private Long tiendaId;
}