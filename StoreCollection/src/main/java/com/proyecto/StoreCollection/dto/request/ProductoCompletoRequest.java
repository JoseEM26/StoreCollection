package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
public class ProductoCompletoRequest {
    private Integer id;
    private Integer tiendaId;

    @NotBlank
    private String nombre;
    @NotBlank private String slug;
    @NotNull private Integer categoriaId;
    @NotEmpty private List<VarianteCompletaRequest> variantes;
}