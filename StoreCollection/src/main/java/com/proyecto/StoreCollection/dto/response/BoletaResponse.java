package com.proyecto.StoreCollection.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;  // ← ¡Importante!
import java.util.List;

@Data
public class BoletaResponse {
    private Integer id;
    private String sessionId;
    private Integer userId;
    private String tiendaNombre;
    private Integer tiendaId;
    private BigDecimal total;

    private LocalDateTime fecha;  // ← Cambia de String a LocalDateTime

    private String estado;
    private List<BoletaDetalleResponse> detalles;

    private String compradorNombre;
    private String compradorEmail;
    private String compradorTelefono;
    private String direccionEnvio;
    private String referenciaEnvio;
    private String distrito;
    private String provincia;
    private String departamento;
    private String codigoPostal;
    private String tipoEntrega;
}