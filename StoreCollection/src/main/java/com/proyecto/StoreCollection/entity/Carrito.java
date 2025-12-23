package com.proyecto.StoreCollection.entity;

import lombok.*;
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

    @ManyToOne
    @JoinColumn(name = "variante_id", nullable = false)
    private ProductoVariante variante;

    @PositiveOrZero
    private Integer cantidad = 1;
}