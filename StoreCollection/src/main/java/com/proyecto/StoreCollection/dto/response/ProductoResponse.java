package com.proyecto.StoreCollection.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.*;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// src/main/java/com/proyecto/StoreCollection/dto/response/ProductoResponse.java
@Getter
@Setter @NoArgsConstructor @AllArgsConstructor
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private Integer categoriaId;

    private List<VarianteResponse> variantes = new ArrayList<>();

    @Getter @Setter
    public static class VarianteResponse {
        private Integer id;
        private String sku;
        private BigDecimal precio;
        private Integer stock;
        private String imagenUrl;
        private boolean activo = true;
        private List<AtributoResponse> atributos = new ArrayList<>();
    }

    @Getter @Setter
    public static class AtributoResponse {
        private String nombreAtributo;
        private String valor;

        public AtributoResponse(String nombreAtributo, String valor) {
            this.nombreAtributo = nombreAtributo;
            this.valor = valor;
        }
    }
}