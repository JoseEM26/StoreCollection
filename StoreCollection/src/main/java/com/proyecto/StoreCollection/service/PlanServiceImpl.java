package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.PlanRequest;
import com.proyecto.StoreCollection.dto.response.PlanResponse;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.repository.PlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanServiceImpl implements PlanService {

    private final PlanRepository repository;

    // ========================================================
    // MÉTODOS DE CONSULTA (Read)
    // ========================================================

    /** Admin: todos los planes activos paginados */
    @Override
    public Page<PlanResponse> findAll(Pageable pageable) {
        return repository.findByActivoTrue(pageable)
                .map(this::toResponse);
    }

    /** Público: solo planes activos y visibles al público */
    @Override
    public Page<PlanResponse> findAllPublicos(Pageable pageable) {
        return repository.findByActivoTrueAndEsVisiblePublicoTrue(pageable)
                .map(this::toResponse);
    }

    @Override
    public PlanResponse findById(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado con ID: " + id));
        return toResponse(plan);
    }
    @Override
    public List<DropTownStandar> findDropdownPlanesActivos() {
        return repository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(plan -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(plan.getId());
                    dto.setDescripcion(plan.getNombre()); // Puedes personalizar aquí si quieres
                    // Ejemplo alternativo con precio:
                    // dto.setDescripcion(formatNombreConPrecio(plan));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    /** Planes públicos visibles ordenados por campo 'orden' (para página de precios) */
    @Override
    public List<PlanResponse> findPlanesPublicosVisibles() {
        return repository.findByActivoTrueAndEsVisiblePublicoTrueOrderByOrdenAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================================
    // MÉTODOS DE CRUD (Create, Update, Delete)
    // ========================================================

    @Override
    @Transactional
    public PlanResponse save(PlanRequest request) {
        return save(request, null);
    }

    @Override
    @Transactional
    public PlanResponse save(PlanRequest request, Integer id) {
        Plan plan;

        if (id == null) {
            // CREAR NUEVO
            plan = new Plan();
        } else {
            // EDITAR EXISTENTE
            plan = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));
        }

        String slugNormalizado = normalizarSlug(request.getSlug());
        validarSlugUnico(slugNormalizado, id);

        updateFromRequest(plan, request);
        plan.setSlug(slugNormalizado);

        plan = repository.save(plan);
        return toResponse(plan);
    }

    @Override
    @Transactional
    public PlanResponse toggleActivo(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));

        boolean nuevoEstado = !plan.getActivo();

        // Si se intenta desactivar, verificar suscripciones activas
        if (!nuevoEstado && tieneSuscripcionesActivas(id)) {
            throw new IllegalStateException(
                    "No se puede desactivar el plan porque tiene suscripciones activas. " +
                            "Cancela o migra las suscripciones primero.");
        }

        plan.setActivo(nuevoEstado);
        return toResponse(repository.save(plan));
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        Plan plan = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado: " + id));

        if (tieneSuscripcionesActivas(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el plan porque tiene suscripciones activas. " +
                            "Cancela o migra las suscripciones primero.");
        }

        repository.delete(plan);
    }

    // ========================================================
    // MÉTODOS DE UTILIDAD INTERNA
    // ========================================================

    public Plan findPlanPorDefecto() {
        return repository.findFirstByActivoTrueAndEsVisiblePublicoTrueOrderByPrecioMensualAsc()
                .orElseThrow(() -> new IllegalStateException("No hay planes activos y visibles disponibles"));
    }

    public List<Plan> findPlanesParaUpgrade(Integer planActualId) {
        Plan actual = repository.findById(planActualId)
                .orElseThrow(() -> new EntityNotFoundException("Plan actual no encontrado"));

        return repository.findByActivoTrueAndEsVisiblePublicoTrueAndPrecioMensualGreaterThan(
                actual.getPrecioMensual(),
                Sort.by(Sort.Direction.ASC, "precioMensual")
        );
    }

    // ========================================================
    // MÉTODOS PRIVADOS
    // ========================================================

    private void updateFromRequest(Plan plan, PlanRequest request) {
        plan.setNombre(request.getNombre());
        plan.setDescripcion(request.getDescripcion());
        plan.setPrecioMensual(request.getPrecioMensual() != null ? request.getPrecioMensual() : BigDecimal.ZERO);
        plan.setPrecioAnual(request.getPrecioAnual());
        plan.setIntervaloBilling(request.getIntervaloBilling());
        plan.setIntervaloCantidad(request.getIntervaloCantidad() != null ? request.getIntervaloCantidad() : 1);
        plan.setDuracionDias(request.getDuracionDias());
        plan.setMaxProductos(request.getMaxProductos() != null ? request.getMaxProductos() : 100);
        plan.setMaxVariantes(request.getMaxVariantes() != null ? request.getMaxVariantes() : 500);
        plan.setEsTrial(request.getEsTrial() != null ? request.getEsTrial() : false);
        plan.setDiasTrial(request.getDiasTrial() != null ? request.getDiasTrial() : (short) 0);
        plan.setEsVisiblePublico(request.getEsVisiblePublico() != null ? request.getEsVisiblePublico() : true);
        plan.setOrden(request.getOrden() != null ? request.getOrden() : (short) 999);
        plan.setActivo(request.getActivo() != null ? request.getActivo() : true);
    }

    private PlanResponse toResponse(Plan p) {
        PlanResponse dto = new PlanResponse();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setSlug(p.getSlug());
        dto.setDescripcion(p.getDescripcion());
        dto.setPrecioMensual(p.getPrecioMensual());
        dto.setPrecioAnual(p.getPrecioAnual());
        dto.setIntervaloBilling(p.getIntervaloBilling());
        dto.setIntervaloCantidad(p.getIntervaloCantidad());
        dto.setDuracionDias(p.getDuracionDias());
        dto.setMaxProductos(p.getMaxProductos());
        dto.setMaxVariantes(p.getMaxVariantes());
        dto.setEsTrial(p.getEsTrial());
        dto.setDiasTrial(p.getDiasTrial());
        dto.setEsVisiblePublico(p.getEsVisiblePublico());
        dto.setOrden(p.getOrden());
        dto.setActivo(p.getActivo());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    private String formatNombreConPrecio(Plan plan) {
        String precio = plan.getPrecioMensual().toPlainString() + " S/ mes";
        if (plan.getPrecioAnual() != null && plan.getPrecioAnual().compareTo(BigDecimal.ZERO) > 0) {
            precio += " | " + plan.getPrecioAnual().toPlainString() + " S/ año";
        }
        return plan.getNombre() + " (" + precio + ")";
    }

    private String normalizarSlug(String slug) {
        return slug.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("[-]+", "-");
    }

    private void validarSlugUnico(String slug, Integer idActual) {
        boolean existe = (idActual == null)
                ? repository.existsBySlug(slug)
                : repository.existsBySlugAndIdNot(slug, idActual);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un plan con el slug: " + slug);
        }
    }

    private boolean tieneSuscripcionesActivas(Integer planId) {
        // TODO: Implementar cuando exista TiendaSuscripcionRepository
        // Ejemplo esperado:
        // return suscripcionRepository.existsByPlanIdAndEstadoIn(planId, Set.of("active", "trialing", "past_due"));
        // Por ahora, retorna false para permitir desarrollo inicial
        return false;
    }
}