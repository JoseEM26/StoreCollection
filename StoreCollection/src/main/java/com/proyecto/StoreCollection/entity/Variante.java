package com.proyecto.StoreCollection.entity;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "variante")
@Data @NoArgsConstructor @AllArgsConstructor
public class Variante {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable = false)
    private Producto producto;

    @NotBlank @Column(unique = true)
    private String sku;

    @Positive
    private BigDecimal precio;

    @PositiveOrZero
    private Integer stock = 0;

    private String imagen;

    @ManyToMany
    @JoinTable(
            name = "variante_atributo",
            joinColumns = @JoinColumn(name = "variante_id"),
            inverseJoinColumns = @JoinColumn(name = "valor_id")
    )
    private Set<AtributoValor> valores = new HashSet<>();
}
