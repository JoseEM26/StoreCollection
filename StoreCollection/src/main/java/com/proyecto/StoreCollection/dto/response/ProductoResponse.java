package com.proyecto.StoreCollection.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private Integer categoriaId;
    private String categoriaNombre;
    private Integer tiendaId;
    private boolean activo;
    private List<VarianteResponse> variantes;


}