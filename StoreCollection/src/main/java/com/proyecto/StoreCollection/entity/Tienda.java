package com.proyecto.StoreCollection.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
// 3. Tienda.java
@Entity
@Table(name = "tienda")
@Data @NoArgsConstructor @AllArgsConstructor
public class Tienda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    private String nombre;

    @NotBlank @Column(unique = true)
    private String slug;

    private String whatsapp;

    @Enumerated(EnumType.STRING)
    private Moneda moneda = Moneda.SOLES;


    private String descripcion;
    private String direccion;
    private String horarios;

    @ManyToOne
    private Plan plan;

    @ManyToOne @JoinColumn(nullable = false)
    private Usuario user;

    public enum Moneda { SOLES, DOLARES }
}