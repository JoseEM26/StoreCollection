package com.proyecto.StoreCollection.dto.special;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanUsageDto(
        // Información básica del plan
        String planNombre,
        BigDecimal precioMensual,

        // Tipo de facturación y renovación
        String intervaloBilling,          // "month" o "year" (o cualquier otro valor que uses)
        LocalDateTime fechaProximaRenovacion,  // Fecha estimada de próximo cobro/renovación
        long diasRestantesRenovacion,          // Días hasta la renovación (-1 si no aplica)
        boolean proximoVencimientoCerca,       // true si faltan ≤ 7 días (para alertas)

        // Límites del plan
        int maxProductos,
        int maxVariantes,

        // Trial (permanecen iguales)
        boolean esTrial,
        int diasTrial,                        // 0 si no es trial
        LocalDateTime fechaInicioTrial,       // null si no es trial
        LocalDateTime fechaFinTrial,          // null si no es trial o indefinido

        // Uso actual
        int productosActuales,
        int variantesActuales,

        // Porcentajes calculados (listos para barras de progreso en frontend)
        double porcentajeProductos,     // 0.0 a 100.0
        double porcentajeVariantes,
        double porcentajeTiempoTrial    // 0.0 a 100.0 (solo si es trial)
) {}