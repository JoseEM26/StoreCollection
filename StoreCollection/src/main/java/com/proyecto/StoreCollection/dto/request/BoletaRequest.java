package com.proyecto.StoreCollection.dto.request;

import com.proyecto.StoreCollection.entity.Boleta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
@Data
public class BoletaRequest {

    @NotBlank(message = "Session ID es requerido")
    private String sessionId;

    private Integer userId;

    @NotNull(message = "Tienda es requerida")
    private Integer tiendaId;

    // Todos opcionales para checkout WhatsApp rápido
    @Size(max = 100)
    private String compradorNombre;

    @Size(max = 120)
    @Email
    private String compradorEmail;

    @Size(max = 20)
    private String compradorNumero;

    // Dirección opcional
    @Size(max = 255)
    private String direccionEnvio;

    @Size(max = 255)
    private String referenciaEnvio;

    @Size(max = 100)
    private String distrito;

    @Size(max = 100)
    private String provincia;

    @Size(max = 60)
    private String departamento;

    private String codigoPostal;

    private String tipoEntrega;  // "DOMICILIO", "RECOGIDA_EN_TIENDA", "AGENCIA"
}