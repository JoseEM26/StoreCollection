package com.proyecto.StoreCollection.entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "carrito")
@Data @NoArgsConstructor @AllArgsConstructor
public class Carrito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "session_id")
    private String sessionId;

    // CAMBIA ESTO:
    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;  // ← ahora sí existe

    @PositiveOrZero
    private Integer cantidad = 1;
}