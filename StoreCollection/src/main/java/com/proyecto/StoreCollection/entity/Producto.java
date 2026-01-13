package com.proyecto.StoreCollection.entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "producto", uniqueConstraints = @UniqueConstraint(columnNames = {"slug", "tienda_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;


    @ManyToOne @JoinColumn(nullable = false)
    @NotNull(message = "La categoría es obligatoria")
    private Categoria categoria;

    @ManyToOne @JoinColumn(nullable = false)
    private Tienda tienda;
    @Column(nullable = false)
    private boolean activo = true;  // por defecto activo
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductoVariante> variantes = new HashSet<>();
}