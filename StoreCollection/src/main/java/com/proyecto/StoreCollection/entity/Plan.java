package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String nombre;

    @NotBlank
    @Column(unique = true)
    private String slug;

    private String descripcion;

    @PositiveOrZero(message = "El precio mensual debe ser mayor o igual a cero")
    @Column(name = "precio_mensual")
    private BigDecimal precioMensual = BigDecimal.ZERO;

    // ← QUITA @PositiveOrZero de precioAnual porque es opcional
    @Column(name = "precio_anual")
    private BigDecimal precioAnual;  // null = sin plan anual

    @NotBlank
    @Column(name = "intervalo_billing")
    private String intervaloBilling = "month";

    // ← QUITA @Min(1) si puede ser null, o déjalo pero permite null con validación personalizada
    @Min(value = 1, message = "La cantidad de intervalos debe ser al menos 1")
    @Column(name = "intervalo_cantidad")
    private Integer intervaloCantidad = 1;

    @Column(name = "duracion_dias")
    private Integer duracionDias;  // null = lifetime → sin validación numérica

    @PositiveOrZero(message = "Máximo productos debe ser mayor o igual a cero")
    @Column(name = "max_productos")
    private Integer maxProductos = 100;

    @PositiveOrZero(message = "Máximo variantes debe ser mayor o igual a cero")
    @Column(name = "max_variantes")
    private Integer maxVariantes = 500;

    @Column(name = "es_trial")
    private Boolean esTrial = false;

    @Column(name = "dias_trial")
    private Short diasTrial = 0;

    @Column(name = "es_visible_publico")
    private Boolean esVisiblePublico = true;

    @Column(name = "orden")
    private Short orden = 999;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Opcional: pre-update para mantener updated_at
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}