package com.proyecto.StoreCollection.entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
// 9. Carrito.java
@Entity
@Table(name = "carrito")
@Data @NoArgsConstructor @AllArgsConstructor
public class Carrito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String sessionId;

    @ManyToOne
    private Variante variante;

    @Positive
    private Integer cantidad = 1;
}