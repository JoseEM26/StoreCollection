package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.special.BoletaDashboardDto;
import com.proyecto.StoreCollection.dto.special.PlanDashboardDto;
import com.proyecto.StoreCollection.dto.special.PlanUsageDto;
import com.proyecto.StoreCollection.dto.special.TiendaDashboardDto;
import com.proyecto.StoreCollection.entity.Boleta;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/dashboard")
public class DashboardController {

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String userEmail = auth.getName();

        Map<String, Object> stats = new HashMap<>();

        if (esAdmin) {
            long totalTiendas = tiendaRepository.count();
            long totalPlanesActivos = planRepository.countByActivoTrue();
            long totalBoletas = boletaRepository.count();

            // Solo boletas ATENDIDAS - USAR EL ENUM
            BigDecimal revenueTotal = boletaRepository.sumTotalByEstado(
                    Boleta.EstadoBoleta.ATENDIDA
            );

            stats.put("totalTiendas", totalTiendas);
            stats.put("totalPlanesActivos", totalPlanesActivos);
            stats.put("totalBoletas", totalBoletas);
            stats.put("revenueTotal", revenueTotal);
        } else {
            List<Tienda> misTiendas = tiendaRepository.findByUserEmailWithPlan(userEmail);
            if (misTiendas.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No tienes tiendas asociadas"));
            }

            long totalTiendas = misTiendas.size();

            long totalPlanesActivos = misTiendas.stream()
                    .map(Tienda::getPlan)
                    .filter(Plan::getActivo)
                    .distinct()
                    .count();

            List<Integer> misTiendaIds = misTiendas.stream()
                    .map(Tienda::getId)
                    .collect(Collectors.toList());

            long totalBoletas = boletaRepository.countByTiendaIdIn(misTiendaIds);

            // Solo boletas ATENDIDAS de MIS tiendas - USAR EL ENUM
            BigDecimal revenueTotal = boletaRepository.sumTotalByEstadoAndTiendaIdIn(
                    Boleta.EstadoBoleta.ATENDIDA,
                    misTiendaIds
            );

            stats.put("totalTiendas", totalTiendas);
            stats.put("totalPlanesActivos", totalPlanesActivos);
            stats.put("totalBoletas", totalBoletas);
            stats.put("revenueTotal", revenueTotal);
        }

        stats.put("rol", esAdmin ? "ADMIN" : "OWNER");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/usage")
    public ResponseEntity<?> getPlanUsage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin) {
            return ResponseEntity.ok(Map.of(
                    "message", "Los administradores no tienen límites de plan",
                    "isAdmin", true
            ));
        }

        // Obtenemos la tienda con su plan (JOIN FETCH para evitar LazyInitializationException)
        List<Tienda> misTiendas = tiendaRepository.findByUserEmailWithPlan(userEmail);

        if (misTiendas.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No tienes tiendas asociadas"
            ));
        }

        Tienda tienda = misTiendas.get(0); // ← asumimos 1 tienda por usuario por ahora
        Plan plan = tienda.getPlan();

        // Conteos actuales
        int productosActuales = productoRepository.countByTiendaId(tienda.getId());
        int variantesActuales = productoVarianteRepository.countByTiendaId(tienda.getId());

        // Porcentajes de uso
        double porcentajeProductos = (plan.getMaxProductos() != null && plan.getMaxProductos() > 0)
                ? Math.min(100.0, (double) productosActuales / plan.getMaxProductos() * 100)
                : 0.0;

        double porcentajeVariantes = (plan.getMaxVariantes() != null && plan.getMaxVariantes() > 0)
                ? Math.min(100.0, (double) variantesActuales / plan.getMaxVariantes() * 100)
                : 0.0;

        // ── Trial ────────────────────────────────────────────────────────────────
        Boolean esTrial = plan.getEsTrial();
        Short diasTrial = plan.getDiasTrial();
        LocalDateTime fechaInicioTrial = null;
        LocalDateTime fechaFinTrial = null;
        double porcentajeTiempoTrial = 0.0;

        if (Boolean.TRUE.equals(esTrial) && diasTrial != null && diasTrial > 0) {
            fechaInicioTrial = tienda.getCreatedAt();
            fechaFinTrial = fechaInicioTrial.plusDays(diasTrial);

            long diasTranscurridos = Duration.between(fechaInicioTrial, LocalDateTime.now()).toDays();
            porcentajeTiempoTrial = diasTrial > 0
                    ? Math.min(100.0, (double) diasTranscurridos / diasTrial * 100)
                    : 0.0;
        }

        // ── Renovación / Vencimiento ─────────────────────────────────────────────
        LocalDateTime fechaVencimientoActual = tienda.getFechaVencimiento();
        LocalDateTime fechaProximaRenovacion = null;
        long diasRestantes = -1;
        boolean vencimientoCerca = false;

        if (fechaVencimientoActual != null) {
            // Días restantes hasta el vencimiento ACTUAL (lo que más le importa al usuario)
            diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), fechaVencimientoActual);
            vencimientoCerca = diasRestantes >= 0 && diasRestantes <= 7;

            // Calculamos la próxima renovación (solo informativo)
            String intervalo = plan.getIntervaloBilling();
            if (intervalo != null) {
                if ("month".equalsIgnoreCase(intervalo) || "monthly".equalsIgnoreCase(intervalo)) {
                    fechaProximaRenovacion = fechaVencimientoActual;
                } else if ("year".equalsIgnoreCase(intervalo) || "annual".equalsIgnoreCase(intervalo)) {
                    fechaProximaRenovacion = fechaVencimientoActual;
                }
            }
        }

        // ── Construcción del DTO ─────────────────────────────────────────────────
        PlanUsageDto usage = new PlanUsageDto(
                plan.getNombre(),
                plan.getPrecioMensual(),
                plan.getIntervaloBilling(),
                fechaProximaRenovacion,           // próxima renovación (informativo)
                diasRestantes,                    // ← DÍAS HASTA VENCIMIENTO ACTUAL
                vencimientoCerca,                 // alerta cuando ≤ 7 días
                plan.getMaxProductos(),
                plan.getMaxVariantes(),
                Boolean.TRUE.equals(esTrial),
                diasTrial,
                fechaInicioTrial,
                fechaFinTrial,
                productosActuales,
                variantesActuales,
                porcentajeProductos,
                porcentajeVariantes,
                porcentajeTiempoTrial
        );

        return ResponseEntity.ok(usage);
    }
    @GetMapping("/tiendas")
    public ResponseEntity<?> getTiendas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String userEmail = auth.getName();

        if (esAdmin) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
            Page<Tienda> tiendasPage = tiendaRepository.findByActivoTrueWithPlan(pageable);

            List<TiendaDashboardDto> dtos = tiendasPage.stream()
                    .map(t -> new TiendaDashboardDto(
                            t.getId(),
                            t.getNombre(),
                            t.getSlug(),
                            t.getActivo(),
                            t.getPlan().getNombre(),
                            t.getPlan().getPrecioMensual(),
                            t.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(new PageImpl<>(dtos, pageable, tiendasPage.getTotalElements()));
        } else {
            List<Tienda> misTiendas = tiendaRepository.findByUserEmailWithPlan(userEmail);

            List<TiendaDashboardDto> dtos = misTiendas.stream()
                    .map(t -> new TiendaDashboardDto(
                            t.getId(),
                            t.getNombre(),
                            t.getSlug(),
                            t.getActivo(),
                            t.getPlan().getNombre(),
                            t.getPlan().getPrecioMensual(),
                            t.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(dtos);
        }
    }
        @GetMapping("/planes")
        public ResponseEntity<?> getPlanes(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String userEmail = auth.getName();

            if (esAdmin) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("orden").ascending());
                Page<Plan> planesPage = planRepository.findByActivoTrueAndEsVisiblePublicoTrue(pageable);

                List<PlanDashboardDto> dtos = planesPage.stream()
                        .map(p -> new PlanDashboardDto(
                                p.getId(),
                                p.getNombre(),
                                p.getSlug(),
                                p.getPrecioMensual(),
                                p.getActivo(),
                                p.getEsVisiblePublico(),
                                p.getOrden()
                        ))
                        .toList();

                return ResponseEntity.ok(new PageImpl<>(dtos, pageable, planesPage.getTotalElements()));
            } else {
                List<Tienda> misTiendas = tiendaRepository.findByUserEmail(userEmail);
                List<PlanDashboardDto> dtos = misTiendas.stream()
                        .map(Tienda::getPlan)
                        .filter(Plan::getActivo)
                        .distinct()
                        .map(p -> new PlanDashboardDto(
                                p.getId(),
                                p.getNombre(),
                                p.getSlug(),
                                p.getPrecioMensual(),
                                p.getActivo(),
                                p.getEsVisiblePublico(),
                                p.getOrden()
                        ))
                        .toList();

                return ResponseEntity.ok(dtos);
            }
        }

        @GetMapping("/boletas")
        public ResponseEntity<?> getBoletas(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String userEmail = auth.getName();

            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());

            if (esAdmin) {
                Page<Boleta> boletasPage = boletaRepository.findAll(pageable);

                List<BoletaDashboardDto> dtos = boletasPage.stream()
                        .map(b -> new BoletaDashboardDto(
                                b.getId(),
                                b.getCompradorNombre(),
                                b.getCompradorEmail(),
                                b.getTotal(),
                                b.getEstado(),
                                b.getFecha(),
                                b.getTienda().getNombre()
                        ))
                        .toList();

                return ResponseEntity.ok(new PageImpl<>(dtos, pageable, boletasPage.getTotalElements()));
            } else {
                List<Tienda> misTiendas = tiendaRepository.findByUserEmail(userEmail);
                List<Integer> misTiendaIds = misTiendas.stream().map(Tienda::getId).collect(Collectors.toList());

                Page<Boleta> todasPage = boletaRepository.findAll(pageable);
                List<BoletaDashboardDto> dtos = todasPage.getContent().stream()
                        .filter(b -> misTiendaIds.contains(b.getTienda().getId()))
                        .map(b -> new BoletaDashboardDto(
                                b.getId(),
                                b.getCompradorNombre(),
                                b.getCompradorEmail(),
                                b.getTotal(),
                                b.getEstado(),
                                b.getFecha(),
                                b.getTienda().getNombre()
                        ))
                        .toList();

                return ResponseEntity.ok(new PageImpl<>(dtos, pageable, todasPage.getTotalElements()));
            }
        }

        @GetMapping("/revenue-por-estado")
        public ResponseEntity<Map<String, BigDecimal>> getRevenuePorEstado() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String userEmail = auth.getName();

            List<Boleta> boletas;

            if (esAdmin) {
                boletas = boletaRepository.findAll();
            } else {
                List<Tienda> misTiendas = tiendaRepository.findByUserEmail(userEmail);
                List<Integer> misTiendaIds = misTiendas.stream().map(Tienda::getId).collect(Collectors.toList());
                boletas = boletaRepository.findAll().stream()
                        .filter(b -> misTiendaIds.contains(b.getTienda().getId()))
                        .toList();
            }

            Map<String, BigDecimal> revenuePorEstado = boletas.stream()
                    .collect(Collectors.groupingBy(
                            b -> b.getEstado().name(),
                            Collectors.mapping(Boleta::getTotal, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            return ResponseEntity.ok(revenuePorEstado);
        }
    }