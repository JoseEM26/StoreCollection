package com.proyecto.StoreCollection.dto.special;
import java.math.BigDecimal;

public record PlanDashboardDto(
        Integer id,
        String nombre,
        String slug,
        BigDecimal precioMensual,
        boolean activo,
        boolean esVisiblePublico,
        int orden
) {}