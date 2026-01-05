package com.proyecto.StoreCollection.dto.special;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TiendaDashboardDto(
        Integer id,
        String nombre,
        String slug,
        boolean activo,
        String planNombre,
        BigDecimal precioMensual,
        LocalDateTime createdAt
) {}