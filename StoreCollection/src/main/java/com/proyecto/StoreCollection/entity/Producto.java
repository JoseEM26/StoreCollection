package com.proyecto.StoreCollection.entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name = "producto", uniqueConstraints = @UniqueConstraint(columnNames = {"slug", "tienda_id"}))
@Data @NoArgsConstructor @AllArgsConstructor
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;

    @ManyToOne @JoinColumn(nullable = false)
    private Categoria categoria;

    @ManyToOne @JoinColumn(nullable = false)
    private Tienda tienda;
}