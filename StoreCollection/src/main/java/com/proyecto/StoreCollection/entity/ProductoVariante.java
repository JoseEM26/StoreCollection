package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "producto_variante")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // SKU único global (o por tienda, según tu regla de negocio)
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 100)
    @Column(unique = true, nullable = false, length = 100)
    private String sku;

    @Positive(message = "El precio debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    private BigDecimal precio_anterior;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private boolean activo = true;

    private String descripcion_corta;


    // === RELACIÓN CON PRODUCTO ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // === RELACIÓN CON TIENDA - CAMBIO CRÍTICO AQUÍ ===
    // Antes tenías: insertable = false, updatable = false → Hibernate IGNORABA la columna en INSERT
    // Ahora: forzamos que siempre se incluya tienda_id en INSERT y UPDATE
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tienda_id",
            nullable = false,
            insertable = true,   // ← CAMBIO CLAVE: permite INSERT
            updatable = true     // ← CAMBIO CLAVE: permite UPDATE (opcional pero recomendado)
    )
    private Tienda tienda;
    // Fin del cambio crítico

    // === ATRIBUTOS DE LA VARIANTE (Color, Talla, etc.) ===
    @ManyToMany
    @JoinTable(
            name = "Variante_Atributo",
            joinColumns = @JoinColumn(name = "variante_id"),
            inverseJoinColumns = @JoinColumn(name = "atributo_valor_id")
    )
    @Builder.Default
    private Set<AtributoValor> atributos = new HashSet<>();

    
}