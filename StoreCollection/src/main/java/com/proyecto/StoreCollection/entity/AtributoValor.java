package com.proyecto.StoreCollection.entity;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "atributo_valor", uniqueConstraints = @UniqueConstraint(columnNames = {"atributo_id", "valor"}))
@Data @NoArgsConstructor @AllArgsConstructor
public class    AtributoValor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atributo_id", nullable = false)
    private Atributo atributo;

    // ← AÑADE ESTO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @NotBlank
    private String valor;
}