package com.proyecto.StoreCollection.entity;

import lombok.*;
        import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boleta")
@Data @NoArgsConstructor @AllArgsConstructor
public class Boleta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario user;

    @ManyToOne
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;  // ← CAMBIO A BIGDECIMAL

    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoletaDetalle> detalles = new ArrayList<>();

    public enum EstadoBoleta {
        PENDIENTE, COMPLETADA, CANCELADA
    }
}