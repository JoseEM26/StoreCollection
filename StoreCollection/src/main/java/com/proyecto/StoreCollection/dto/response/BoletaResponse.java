package com.proyecto.StoreCollection.dto.response;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BoletaResponse {
    private Integer id;
    private String sessionId;
    private Integer userId;
    private Integer tiendaId;
    private BigDecimal total;
    private String fecha;
    private String estado;
    private List<BoletaDetalleResponse> detalles;
}