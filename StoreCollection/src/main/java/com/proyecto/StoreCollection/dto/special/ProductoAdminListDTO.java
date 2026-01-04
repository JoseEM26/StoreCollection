package com.proyecto.StoreCollection.dto.special;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoAdminListDTO {

    private Integer id;
    private String nombre;
    private String slug;
    private Integer categoriaId;
    private String categoriaNombre;
    private Integer tiendaId;
    private String tiendaNombre;       // ← muy útil para admin
    private boolean activo;

    private BigDecimal precioMinimo;
    private BigDecimal precioMaximo;
    private Integer stockTotal;
    private String imagenPrincipal;

    private boolean tieneVariantes;
    private int cantidadVariantes;

    // Opcional: solo si realmente lo necesitas en la lista
    // private List<VarianteMiniDTO> variantesResumen; // máximo 2-3 elementos
}