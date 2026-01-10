package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Datos del comprador - todos opcionales
    @Size(max = 100)
    private String compradorNombre;

    @Email
    @Size(max = 120)
    private String compradorEmail;

    @Size(max = 20)
    private String compradorNumero;          // ← nombre consistente con BD

    // Dirección de envío - todos opcionales
    @Size(max = 255)
    private String direccionEnvio;

    @Size(max = 255)
    private String referenciaEnvio;

    @Size(max = 100)
    private String distrito;

    @Size(max = 100)
    private String provincia;

    @Size(max = 60)
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

    // Helper útil para mostrar dirección
    public String getDireccionCompleta() {
        if (direccionEnvio == null) return "Por coordinar vía WhatsApp";
        StringBuilder sb = new StringBuilder(direccionEnvio);
        if (StringUtils.hasText(referenciaEnvio)) sb.append(" - ").append(referenciaEnvio);
        if (StringUtils.hasText(distrito)) sb.append(", ").append(distrito);
        return sb.toString().trim();
    }
}