package com.proyecto.StoreCollection.dto.special;
import com.proyecto.StoreCollection.entity.Boleta.EstadoBoleta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BoletaDashboardDto(
        Integer id,
        String compradorNombre,
        BigDecimal total,
        EstadoBoleta estado,
        LocalDateTime fecha,
        String tiendaNombre
) {}