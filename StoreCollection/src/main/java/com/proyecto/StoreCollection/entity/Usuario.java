package com.proyecto.StoreCollection.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Data @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @Email @NotBlank @Column(unique = true)
    private String email;

    @NotBlank
    private String password; // sin hashear por ahora

    private String celular;

    @Enumerated(EnumType.STRING)
    private Rol rol = Rol.CUSTOMER;

    public enum Rol { ADMIN, OWNER, CUSTOMER }
}