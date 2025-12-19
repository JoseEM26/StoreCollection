package com.proyecto.StoreCollection.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "atributo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "tienda_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Size(max = 50)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    // === RELACIÓN CON LOS VALORES - ESTO ES LO QUE FALTABA ===
    @OneToMany(
            mappedBy = "atributo",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<AtributoValor> valores = new ArrayList<>();
}