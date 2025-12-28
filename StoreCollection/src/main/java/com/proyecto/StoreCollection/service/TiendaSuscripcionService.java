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
import java.util.Set;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TiendaSuscripcionService {

    private final TiendaSuscripcionRepository suscripcionRepository;
    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;

    // Estados considerados "vigentes"
    private static final Set<String> ESTADOS_VIGENTES = Set.of("trial", "active", "grace");
    private static final Set<String> ESTADOS_ACTIVOS_O_TRIAL = Set.of("trial", "active");

    @Transactional
    public TiendaSuscripcion crearSuscripcionInicial(Integer tiendaId, Integer planId) {
        Tienda tienda = obtenerTienda(tiendaId);
        Plan plan = obtenerPlan(planId);

        if (tieneSuscripcionVigente(tiendaId)) {
            throw new IllegalStateException("La tienda ya tiene una suscripción vigente");
        }

        TiendaSuscripcion suscripcion = new TiendaSuscripcion();
        suscripcion.setTienda(tienda);
        suscripcion.setPlan(plan);
        suscripcion.setEstado(plan.getEsTrial() == Boolean.TRUE && plan.getDiasTrial() > 0 ? "trial" : "active");
        suscripcion.setFechaInicio(LocalDateTime.now());
        suscripcion.setPaymentProvider("manual");
        suscripcion.setNotas("Suscripción inicial creada");

        configurarFechas(suscripcion, plan);
        return suscripcionRepository.save(suscripcion);
    }

    @Transactional
    public TiendaSuscripcion cambiarPlan(Integer tiendaId, Integer nuevoPlanId, String provider, String externalId) {
        TiendaSuscripcion suscripcion = obtenerSuscripcionVigente(tiendaId)
                .orElseThrow(() -> new IllegalStateException("No hay suscripción activa para cambiar plan"));

        Plan nuevoPlan = obtenerPlan(nuevoPlanId);

        // Actualizar plan y proveedor
        suscripcion.setPlan(nuevoPlan);
        suscripcion.setPaymentProvider(provider != null ? provider : suscripcion.getPaymentProvider());
        suscripcion.setExternalSubscriptionId(externalId);
        suscripcion.setEstado("active"); // sale del trial si estaba
        suscripcion.setNotas("Cambio de plan a " + nuevoPlan.getNombre());

        configurarFechas(suscripcion, nuevoPlan);
        return suscripcionRepository.save(suscripcion);
    }

    @Transactional
    public TiendaSuscripcion cancelarSuscripcion(Integer tiendaId, boolean cancelacionInmediata, String motivo) {
        TiendaSuscripcion suscripcion = obtenerSuscripcionVigente(tiendaId)
                .orElseThrow(() -> new IllegalStateException("No hay suscripción activa para cancelar"));

        if (cancelacionInmediata) {
            suscripcion.setEstado("cancelled");
            suscripcion.setFechaFin(LocalDateTime.now());
        } else {
            suscripcion.setEstado("active"); // sigue activa hasta fin de período
            suscripcion.setCancelAtPeriodEnd(true);
            suscripcion.setCancelAt(suscripcion.getPeriodoActualFin());
        }

        suscripcion.setNotas("Cancelación solicitada: " + (motivo != null ? motivo : "sin motivo"));
        return suscripcionRepository.save(suscripcion);
    }

    // ======================== CONSULTAS ========================

    @Transactional(readOnly = true)
    public boolean tieneSuscripcionVigente(Integer tiendaId) {
        return suscripcionRepository.existsSuscripcionVigente(
                tiendaId, LocalDateTime.now(), ESTADOS_VIGENTES);
    }

    @Transactional(readOnly = true)
    public Optional<TiendaSuscripcion> obtenerSuscripcionVigente(Integer tiendaId) {
        return suscripcionRepository.findPrimeraSuscripcionVigente(
                tiendaId, LocalDateTime.now(), ESTADOS_VIGENTES);
    }

    @Transactional(readOnly = true)
    public boolean estaEnTrial(Integer tiendaId) {
        return obtenerSuscripcionVigente(tiendaId)
                .map(s -> "trial".equals(s.getEstado()))
                .orElse(false);
    }

    // ======================== PRIVADOS ========================

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

        // Trial
        if ("trial".equals(sus.getEstado()) && plan.getDiasTrial() > 0) {
            LocalDateTime finTrial = ahora.plusDays(plan.getDiasTrial());
            sus.setTrialEndsAt(finTrial);
            sus.setPeriodoActualFin(finTrial);
            sus.setFechaFin(finTrial);
            return;
        }

        // Duración fija
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