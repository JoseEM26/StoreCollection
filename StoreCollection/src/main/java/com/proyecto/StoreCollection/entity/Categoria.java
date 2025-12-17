package com.proyecto.StoreCollection.entity;

import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(columnNames = {"slug", "tienda_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Tienda tienda;


}