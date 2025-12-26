package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
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

    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario user;  // null para guest checkout

    @ManyToOne
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    private LocalDateTime fecha = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    // ── DATOS DEL COMPRADOR ────────────────────────────────────────────────

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String compradorNombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String compradorEmail;

    @Pattern(regexp = "^\\+?\\d{1,3}?[-.\\s]?\\(?\\d{1,4}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}$",
            message = "Formato de teléfono inválido (ej: +51 999 123 456 o 999123456)")
    @Size(max = 20)
    private String compradorTelefono;  // opcional pero con formato validado

    // ── DATOS DE ENVÍO ─────────────────────────────────────────────────────

    @NotBlank(message = "La dirección de envío es obligatoria")
    @Size(min = 5, max = 150, message = "La dirección debe tener entre 5 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String direccionEnvio;

    @Size(max = 100, message = "La referencia debe tener máximo 100 caracteres")
    private String referenciaEnvio;  // Ej: "Frente al parque", "Casa verde", "Edificio Apto 302"

    @NotBlank(message = "El distrito es obligatorio")
    @Size(min = 2, max = 60, message = "El distrito debe tener entre 2 y 60 caracteres")
    @Column(nullable = false, length = 60)
    private String distrito;

    @NotBlank(message = "La provincia es obligatoria")
    @Size(min = 2, max = 60, message = "La provincia debe tener entre 2 y 60 caracteres")
    @Column(nullable = false, length = 60)
    private String provincia;

    @NotBlank(message = "El departamento es obligatorio")
    @Size(min = 2, max = 40, message = "El departamento debe tener entre 2 y 40 caracteres")
    @Column(nullable = false, length = 40)
    private String departamento;  // Ej: Lima, Arequipa, La Libertad...

    // Opcional: código postal (no muy usado en Perú, pero útil para algunos couriers)
    @Size(max = 10)
    private String codigoPostal;

    // Opcional: tipo de entrega (puedes usar enum)
    @Enumerated(EnumType.STRING)
    private TipoEntrega tipoEntrega = TipoEntrega.DOMICILIO;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoletaDetalle> detalles = new ArrayList<>();

    public enum EstadoBoleta {
        PENDIENTE, ATENDIDA, CANCELADA
    }

    public enum TipoEntrega {
        DOMICILIO,     // Envío a casa/oficina
        RECOGIDA_EN_TIENDA,  // Recojo en local
        AGENCIA        // Envío a agencia (Olva, Shalom, etc.)
    }

    // Helpers útiles
    public boolean esGuest() {
        return user == null;
    }

    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder(direccionEnvio);
        if (referenciaEnvio != null && !referenciaEnvio.isBlank()) {
            sb.append(" - ").append(referenciaEnvio);
        }
        sb.append(", ").append(distrito)
                .append(", ").append(provincia)
                .append(", ").append(departamento);
        if (codigoPostal != null && !codigoPostal.isBlank()) {
            sb.append(" - CP: ").append(codigoPostal);
        }
        return sb.toString();
    }
}