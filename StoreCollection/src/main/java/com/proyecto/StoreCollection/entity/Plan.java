package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan")
@Data @NoArgsConstructor @AllArgsConstructor
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @PositiveOrZero
    private BigDecimal precio = BigDecimal.ZERO;

    @Positive
    private Integer maxProductos = 100;

    @Min(1) @Max(12)
    @Column(name = "mes_inicio", nullable = false)
    private Integer mesInicio;

    @Min(1) @Max(12)
    @Column(name = "mes_fin", nullable = false)
    private Integer mesFin;
}