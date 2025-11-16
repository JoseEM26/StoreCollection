package com.proyecto.StoreCollection.entity;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
// 4. Categoria.java
@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(columnNames = {"slug", "tienda_id"}))
@Data @NoArgsConstructor @AllArgsConstructor
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;

    @ManyToOne @JoinColumn(nullable = false)
    private Tienda tienda;
}