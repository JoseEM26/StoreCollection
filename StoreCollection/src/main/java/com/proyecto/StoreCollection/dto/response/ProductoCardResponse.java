package com.proyecto.StoreCollection.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCardResponse {

    private Integer id;
    private String nombre;
    private String slug;
    private String nombreCategoria;

    @Builder.Default
    private BigDecimal precioMinimo = BigDecimal.ZERO;

    @Builder.Default
    private Integer stockTotal = 0;

    private String imagenPrincipal;

    @Builder.Default
    private List<VarianteCard> variantes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarianteCard {
        private Integer id;
        private BigDecimal precio;
        private Integer stock;
        private String imagenUrl;
        @Builder.Default
        private boolean activo = true;

        // ← NUEVO: Lista de atributos
        @Builder.Default
        private List<AtributoValorDTO> atributos = new ArrayList<>();

        // DTO interno simple para atributo-valor
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AtributoValorDTO {
            private String atributoNombre;
            private String valor;
        }
    }
}
