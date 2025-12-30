package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tienda_suscripcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"tienda", "plan"})
public class TiendaSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relación con la tienda (tenant)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    // Relación con el plan seleccionado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // Estado de la suscripción
    @Column(nullable = false, length = 30)
    private String estado; // trial, active, past_due, cancelled, expired, grace, incomplete

    // Fecha de inicio de esta suscripción
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    // Fecha de fin (null = lifetime / indefinido)
    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Para períodos de prueba
    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    // Período de facturación actual
    @Column(name = "periodo_actual_inicio")
    private LocalDateTime periodoActualInicio;

    @Column(name = "periodo_actual_fin")
    private LocalDateTime periodoActualFin;

    // Fecha programada de cancelación (si aplica)
    @Column(name = "cancel_at")
    private LocalDateTime cancelAt;

    // Cancelar al final del período (Stripe style)
    @Column(name = "cancel_at_period_end")
    private boolean cancelAtPeriodEnd = false;

    // Proveedor de pago
    @Column(name = "payment_provider", length = 30)
    private String paymentProvider = "manual"; // stripe, paypal, manual, etc.

    // ID externo (muy importante para webhooks)
    @Column(name = "external_subscription_id", length = 100)
    private String externalSubscriptionId;

    // Notas / motivo de cancelación / comentarios
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Auditoría básica
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -----------------------
    // Métodos de conveniencia
    // -----------------------

    /**
     * Indica si la suscripción está actualmente vigente
     */
    public boolean isVigente() {
        LocalDateTime ahora = LocalDateTime.now();
        return
                (estado.equals("active") || estado.equals("trial") || estado.equals("grace")) &&
                        (fechaFin == null || fechaFin.isAfter(ahora));
    }

    /**
     * Indica si está en período de prueba activo
     */
    public boolean isTrialActivo() {
        return "trial".equals(estado) &&
                trialEndsAt != null &&
                trialEndsAt.isAfter(LocalDateTime.now());
    }

    /**
     * Indica si está programada para cancelarse al final del período
     */
    public boolean isScheduledForCancellation() {
        return cancelAtPeriodEnd && cancelAt != null;
    }
}