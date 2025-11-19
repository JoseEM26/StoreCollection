package com.proyecto.StoreCollection.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private String slug;
    private Long tiendaId;
}