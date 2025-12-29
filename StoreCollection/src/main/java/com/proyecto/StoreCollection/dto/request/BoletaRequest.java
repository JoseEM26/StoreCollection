package com.proyecto.StoreCollection.dto.request;

import com.proyecto.StoreCollection.entity.Boleta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
@Data
public class BoletaRequest {

    @NotBlank
    private String sessionId;

    private Integer userId;

    @NotNull
    private Integer tiendaId;

    @NotBlank
    @Size(min = 3, max = 100)
    private String compradorNombre;

    @NotBlank
    @Email
    @Size(max = 120)
    private String compradorEmail;

    @Size(max = 20)
    private String compradorTelefono;

    // Campos de dirección opcionales
    @Size(max = 150)
    private String direccionEnvio;

    @Size(max = 100)
    private String referenciaEnvio;

    @Size(max = 60)
    private String distrito;

    @Size(max = 60)
    private String provincia;

    @Size(max = 40)
    private String departamento;

    @Size(max = 10)
    private String codigoPostal;

    private Boleta.TipoEntrega tipoEntrega;

    // Opcional
    private String metodoPago;
}