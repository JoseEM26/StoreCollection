package com.proyecto.StoreCollection.entity;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "atributo_valor", uniqueConstraints = @UniqueConstraint(columnNames = {"atributo_id", "valor"}))
@Data @NoArgsConstructor @AllArgsConstructor
public class AtributoValor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(nullable = false)
    private Atributo atributo;

    @NotBlank
    private String valor;
}