package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// ... imports ...

@Entity
@Table(name = "boleta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario user;

    @ManyToOne
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    // Datos del comprador
    @NotBlank
    @Size(min = 3, max = 100)
    private String compradorNombre;

    @Email
    @Size(max = 120)
    private String compradorEmail;

    @Size(max = 20)
    private String compradorTelefono;

    // Campos de dirección → opcionales ahora
    @Size(max = 150)
    private String direccionEnvio;

    @Size(max = 100)
    private String referenciaEnvio;

    @Size(max = 60)
    private String distrito;

    @Size(max = 60)
    private String provincia;

    @Size(max = 40)
    private String departamento;

    @Size(max = 10)
    private String codigoPostal;

    @Enumerated(EnumType.STRING)
    private TipoEntrega tipoEntrega = TipoEntrega.DOMICILIO;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoletaDetalle> detalles = new ArrayList<>();

    public enum EstadoBoleta {
        PENDIENTE, ATENDIDA, CANCELADA
    }

    public enum TipoEntrega {
        DOMICILIO, RECOGIDA_EN_TIENDA, AGENCIA
    }

    // Puedes mantener este helper, pero ya no es crítico
    public String getDireccionCompleta() {
        if (direccionEnvio == null) return "Por coordinar vía WhatsApp";
        StringBuilder sb = new StringBuilder(direccionEnvio);
        if (referenciaEnvio != null && !referenciaEnvio.isBlank()) sb.append(" - ").append(referenciaEnvio);
        if (distrito != null) sb.append(", ").append(distrito);
        return sb.toString();
    }
}