package com.proyecto.StoreCollection.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class VarianteCompletaRequest {
    @NotBlank
    private Integer id;

    @NotBlank
    private String sku;

    @NotNull
    @Positive
    private BigDecimal precio;

    private Integer stock = 0;
    private String imagenUrl;
    private Boolean activo = true;

    private Set<Integer> atributoValorIds; // ej: [1, 5] → Color: Rojo + Talla: M
}