package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producto_variante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false) // AQUÍ ESTABA EL ERROR
    private Tienda tienda;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock = 0;

    private String imagenUrl;

    @Column(nullable = false)
    private Boolean activo = true;

    // MEJOR: usa List en lugar de Set (más control y orden)
    @ManyToMany
    @JoinTable(
            name = "variante_atributo",
            joinColumns = @JoinColumn(name = "variante_id"),
            inverseJoinColumns = @JoinColumn(name = "atributo_valor_id")
    )
    private List<AtributoValor> atributos = new ArrayList<>();
}