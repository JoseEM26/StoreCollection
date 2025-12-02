package com.proyecto.StoreCollection.entity;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "Producto_Variante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotBlank
    @Column(unique = true, length = 100)
    private String sku;

    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock = 0;


    @Column(name = "imagenUrl")
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false, updatable = false, insertable = false)
    private Tienda tienda;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany
    @JoinTable(
            name = "Variante_Atributo",
            joinColumns = @JoinColumn(name = "variante_id"),
            inverseJoinColumns = @JoinColumn(name = "atributo_valor_id")
    )
    private Set<AtributoValor> atributos = new HashSet<>();
}