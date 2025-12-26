package com.proyecto.StoreCollection.dto.request;

import com.proyecto.StoreCollection.entity.Boleta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class BoletaRequest {

    @NotBlank(message = "El ID de sesión es obligatorio")
    private String sessionId;

    private Integer userId; // Opcional - si el usuario está logueado

    @NotNull(message = "El ID de la tienda es obligatorio")
    private Integer tiendaId;

    // Datos del comprador
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "Nombre entre 3 y 100 caracteres")
    private String compradorNombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 120)
    private String compradorEmail;

    @Pattern(regexp = "^\\+?\\d{1,3}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}$",
            message = "Formato de teléfono inválido (ej: +51999123456 o 999123456)")
    private String compradorTelefono; // opcional

    // Datos de envío
    @NotBlank(message = "La dirección de envío es obligatoria")
    @Size(min = 5, max = 150)
    private String direccionEnvio;

    @Size(max = 100)
    private String referenciaEnvio;

    @NotBlank(message = "El distrito es obligatorio")
    @Size(min = 2, max = 60)
    private String distrito;

    @NotBlank(message = "La provincia es obligatoria")
    @Size(min = 2, max = 60)
    private String provincia;

    @NotBlank(message = "El departamento es obligatorio")
    @Size(min = 2, max = 40)
    private String departamento;

    @Size(max = 10)
    private String codigoPostal;

    @NotNull(message = "El tipo de entrega es obligatorio")
    private Boleta.TipoEntrega tipoEntrega;

    // Items (pueden venir del request o cargarse desde el carrito)
    // Si prefieres que el frontend envíe los items, descomenta esto:
    // @NotEmpty(message = "Debe haber al menos un producto")
    // @Valid
    // private List<DetalleBoletaRequest> items;

    // Opcional: método de pago
    private String metodoPago; // ej: CONTRA_ENTREGA, TRANSFERENCIA, TARJETA
}