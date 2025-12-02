package com.proyecto.StoreCollection.entity;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "atributo")
@Data @NoArgsConstructor @AllArgsConstructor
public class Atributo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String nombre;

    @ManyToOne @JoinColumn(nullable = false)
    private Tienda tienda;
}