package com.proyecto.StoreCollection.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCreateResponse {
    private Integer id;

    private String nombre;

    private String slug;

    private Integer categoriaId;

    private String categoriaNombre;

    private Integer tiendaId;

    private String tiendaSlug;

    // === CAMPOS DERIVADOS (muy importantes para el frontend) ===
    private BigDecimal precioMinimo = BigDecimal.ZERO;

    private Integer stockTotal = 0;

    private String imagenPrincipal; // URL de la primera variante activa

    private Integer totalVariantes = 0;

    private Integer variantesActivas = 0;

    // === PARA EL CATÁLOGO PÚBLICO (opcional pero recomendado) ===
    private List<VarianteSimple> variantes = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteSimple {
        private Integer id;
        private String sku;
        private BigDecimal precio;
        private Integer stock;
        private String imagenUrl;
        private boolean activo;
        private List<AtributoSimple> atributos = new ArrayList<>();
    }

    // Para mostrar "Color: Rojo, Talla: M"
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtributoSimple {
        private String nombreAtributo;  // ej: "Color"
        private String valor;           // ej: "Rojo"
    }
}
