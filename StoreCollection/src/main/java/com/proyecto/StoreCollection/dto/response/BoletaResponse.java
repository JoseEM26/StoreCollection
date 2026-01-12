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
    private Integer tiendaId;
    private String tiendaNombre;
    private BigDecimal total;
    private LocalDateTime fecha;           // mejor como objeto, el frontend lo formatea
    private String estado;

    // Datos comprador
    private String compradorNombre;
    private String compradorEmail;
    private String compradorNumero;        // consistente

    // Dirección
    private String direccionEnvio;
    private String referenciaEnvio;
    private String distrito;
    private String provincia;
    private String departamento;
    private String codigoPostal;
    private String tipoEntrega;
    private String ruc;

    private List<BoletaDetalleResponse> detalles;
}