package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VarianteRequest {
    private Integer id;  // Para update, si existe

    @NotBlank
    private String sku;

    @Positive
    private BigDecimal precio;

    private BigDecimal precio_anterior;
    private String descripcion_corta;

    @PositiveOrZero
    private Integer stock = 0;
    private Boolean activo = true;
    private MultipartFile imagen;
    private String imagenUrl;
    private List<AtributoValorRequest> atributos;
}