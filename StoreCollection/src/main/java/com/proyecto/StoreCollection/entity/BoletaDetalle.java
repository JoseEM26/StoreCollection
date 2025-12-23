package com.proyecto.StoreCollection.entity;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "boleta_detalle")
@Data @NoArgsConstructor @AllArgsConstructor
public class BoletaDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;

    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 10, scale = 2)
    private BigDecimal precioUnitario;  // ← CAMBIO A BIGDECIMAL

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;  // ← CAMBIO A BIGDECIMAL
}