package com.proyecto.StoreCollection.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoCardResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private String nombreCategoria;

    private List<VarianteCard> variantes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VarianteCard {
        private BigDecimal precio;
        private Integer stock;
        private String imagenUrl;
        private boolean activo;
    }

    // Métodos que usa directamente tu Angular
    public BigDecimal getPrecioMinimo() {
        if (variantes == null || variantes.isEmpty()) return BigDecimal.ZERO;
        return variantes.stream()
                .filter(VarianteCard::isActivo)
                .map(VarianteCard::getPrecio)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public int getStockTotal() {
        if (variantes == null || variantes.isEmpty()) return 0;
        return variantes.stream()
                .filter(VarianteCard::isActivo)
                .mapToInt(VarianteCard::getStock)
                .sum();
    }

    public String getImagenPrincipal() {
        if (variantes == null) return null;
        return variantes.stream()
                .filter(v -> v.isActivo() && v.getImagenUrl() != null && !v.getImagenUrl().isBlank())
                .map(VarianteCard::getImagenUrl)
                .findFirst()
                .orElse("https://img.freepik.com/vector-premium/no-hay-fotos-ilustracion-plana_120816-197113.jpg");
    }
}