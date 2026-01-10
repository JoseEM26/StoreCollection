package com.proyecto.StoreCollection.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BoletaAdminRequest {
    @NotNull(message = "El ID de la tienda es obligatorio")
    private Integer tiendaId;

    @NotEmpty(message = "Debe agregar al menos un ítem")
    @Size(min = 1, message = "Debe agregar al menos un ítem")
    private List<ItemRequest> items;

    private BigDecimal total;
    private String compradorNombre;
    private String compradorNumero;


    @Data
    public static class ItemRequest {
        @NotNull(message = "El ID de variante es obligatorio")
        private Integer varianteId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        private Integer cantidad;
    }
}