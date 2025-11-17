package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String slug;
    private Long categoriaId;
    private String categoriaNombre;
    private Long tiendaId;
}