package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.TiendaSuscripcion;
import com.proyecto.StoreCollection.repository.PlanRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.repository.TiendaSuscripcionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TiendaSuscripcionService {

    private final TiendaSuscripcionRepository suscripcionRepository;
    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;

    // Estados considerados "vigentes"
    private static final Set<String> ESTADOS_VIGENTES = Set.of("trial", "active", "grace");

    /**
     * Crea la suscripción inicial al crear una nueva tienda
     */
    @Transactional
    public TiendaSuscripcion crearSuscripcionInicial(Integer tiendaId, Integer planId) {
        Tienda tienda = obtenerTienda(tiendaId);
        Plan plan = obtenerPlan(planId);

        if (tieneSuscripcionVigente(tiendaId)) {
            throw new IllegalStateException("La tienda ya tiene una suscripción vigente");
        }

        TiendaSuscripcion suscripcion = TiendaSuscripcion.builder()
                .tienda(tienda)
                .plan(plan)
                .estado(plan.getEsTrial() == Boolean.TRUE && plan.getDiasTrial() > 0 ? "trial" : "active")
                .fechaInicio(LocalDateTime.now())
                .paymentProvider("manual")
                .notas("Suscripción inicial creada automáticamente")
                .build();

        configurarFechas(suscripcion, plan);
        return suscripcionRepository.save(suscripcion);
    }

    /**
     * Método que usaremos desde TiendaService para obtener el plan activo
     */
    @Transactional(readOnly = true)
    public Optional<TiendaSuscripcion> findSuscripcionActiva(Integer tiendaId) {
        LocalDateTime ahora = LocalDateTime.now();
        return suscripcionRepository.findPrimeraSuscripcionVigente(tiendaId, ahora, ESTADOS_VIGENTES);
    }

    /**
     * Cambiar de plan (ej: de Básico a Pro)
     */
    @Transactional
    public TiendaSuscripcion cambiarPlan(Integer tiendaId, Integer nuevoPlanId, String provider, String externalId) {
        TiendaSuscripcion suscripcion = obtenerSuscripcionVigente(tiendaId)
                .orElseThrow(() -> new IllegalStateException("No hay suscripción activa para cambiar plan"));

        Plan nuevoPlan = obtenerPlan(nuevoPlanId);

        suscripcion.setPlan(nuevoPlan);
        if (provider != null) suscripcion.setPaymentProvider(provider);
        suscripcion.setExternalSubscriptionId(externalId);
        suscripcion.setEstado("active");
        suscripcion.setNotas("Cambio de plan a " + nuevoPlan.getNombre());

        configurarFechas(suscripcion, nuevoPlan);
        return suscripcionRepository.save(suscripcion);
    }

    /**
     * Cancelar suscripción
     */
    @Transactional
    public TiendaSuscripcion cancelarSuscripcion(Integer tiendaId, boolean cancelacionInmediata, String motivo) {
        TiendaSuscripcion suscripcion = obtenerSuscripcionVigente(tiendaId)
                .orElseThrow(() -> new IllegalStateException("No hay suscripción activa para cancelar"));

        if (cancelacionInmediata) {
            suscripcion.setEstado("cancelled");
            suscripcion.setFechaFin(LocalDateTime.now());
        } else {
            suscripcion.setEstado("active");
            suscripcion.setCancelAtPeriodEnd(true);
            suscripcion.setCancelAt(suscripcion.getPeriodoActualFin());
        }

        suscripcion.setNotas("Cancelación: " + (motivo != null ? motivo : "sin motivo"));
        return suscripcionRepository.save(suscripcion);
    }

    // ======================== CONSULTAS AUXILIARES ========================

    @Transactional(readOnly = true)
    public boolean tieneSuscripcionVigente(Integer tiendaId) {
        return suscripcionRepository.existsSuscripcionVigente(
                tiendaId, LocalDateTime.now(), ESTADOS_VIGENTES);
    }

    @Transactional(readOnly = true)
    public Optional<TiendaSuscripcion> obtenerSuscripcionVigente(Integer tiendaId) {
        return findSuscripcionActiva(tiendaId); // Reutilizamos el mismo método
    }

    @Transactional(readOnly = true)
    public boolean estaEnTrial(Integer tiendaId) {
        return obtenerSuscripcionVigente(tiendaId)
                .map(s -> "trial".equals(s.getEstado()))
                .orElse(false);
    }

    // ======================== MÉTODOS PRIVADOS ========================

    private Tienda obtenerTienda(Integer id) {
        return tiendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tienda no encontrada: " + id));
    }

    private Plan obtenerPlan(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));
    }

    private void configurarFechas(TiendaSuscripcion sus, Plan plan) {
        LocalDateTime ahora = LocalDateTime.now();
        sus.setPeriodoActualInicio(ahora);

        if ("trial".equals(sus.getEstado()) && plan.getDiasTrial() > 0) {
            LocalDateTime finTrial = ahora.plusDays(plan.getDiasTrial());
            sus.setTrialEndsAt(finTrial);
            sus.setPeriodoActualFin(finTrial);
            sus.setFechaFin(finTrial);
            return;
        }

        if (plan.getDuracionDias() != null && plan.getDuracionDias() > 0) {
            sus.setFechaFin(ahora.plusDays(plan.getDuracionDias()));
        } else {
            sus.setFechaFin(null); // lifetime
        }

        // Período de facturación
        if ("month".equals(plan.getIntervaloBilling())) {
            sus.setPeriodoActualFin(ahora.plusMonths(plan.getIntervaloCantidad()));
        } else if ("year".equals(plan.getIntervaloBilling())) {
            sus.setPeriodoActualFin(ahora.plusYears(plan.getIntervaloCantidad()));
        } else {
            sus.setPeriodoActualFin(sus.getFechaFin());
        }
    }
}