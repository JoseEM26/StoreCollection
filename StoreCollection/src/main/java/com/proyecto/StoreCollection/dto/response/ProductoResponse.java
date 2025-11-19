package com.proyecto.StoreCollection.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class ProductoResponse {
    private Long id;
    private String nombre;
    private String slug;
    private Long categoriaId;
    private String categoriaNombre;   
    private Long tiendaId;

}