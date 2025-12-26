package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BoletaServiceImpl implements BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final PdfService pdfService;
    // ───────────────────────────────────────────────────────────────
    // Listados paginados
    // ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findAll(Pageable pageable) {
        return boletaRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findByTiendaId(Integer tiendaId, Pageable pageable) {
        return boletaRepository.findByTiendaId(tiendaId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findByEstado(String estado, Pageable pageable) {
        Boleta.EstadoBoleta estadoEnum = Boleta.EstadoBoleta.valueOf(estado.toUpperCase());
        return boletaRepository.findByEstado(estadoEnum, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findBySessionId(String sessionId, Pageable pageable) {
        return boletaRepository.findBySessionId(sessionId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findByTiendaIdAndEstado(Integer tiendaId, String estado, Pageable pageable) {
        Boleta.EstadoBoleta estadoEnum = Boleta.EstadoBoleta.valueOf(estado.toUpperCase());
        return boletaRepository.findByTiendaIdAndEstado(tiendaId, estadoEnum, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findByTiendaIdAndSessionId(Integer tiendaId, String sessionId, Pageable pageable) {
        return boletaRepository.findByTiendaIdAndSessionId(tiendaId, sessionId, pageable)
                .map(this::toResponse);
    }

    // ───────────────────────────────────────────────────────────────
    // Detalle con permisos
    // ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse findByIdConPermisos(Integer id) {
        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + id));

        verificarPermisosSobreBoleta(boleta);
        return toResponse(boleta);
    }

    private void verificarPermisosSobreBoleta(Boleta boleta) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin) {
            return;
        }

        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null || !tenantId.equals(boleta.getTienda().getId())) {
            throw new AccessDeniedException("No tienes permisos para ver esta boleta");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Cambio de estado + gestión de stock
    // ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoletaResponse actualizarEstado(Integer id, String estadoStr) {
        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + id));

        verificarPermisosSobreBoleta(boleta);

        // Validar estado recibido
        Boleta.EstadoBoleta nuevoEstado;
        try {
            nuevoEstado = Boleta.EstadoBoleta.valueOf(estadoStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado inválido: '" + estadoStr + "'. Los estados permitidos son: PENDIENTE, ATENDIDA, CANCELADA");
        }

        Boleta.EstadoBoleta estadoAnterior = boleta.getEstado();

        // === GESTIÓN DE STOCK ===
        // Caso 1: Se atiende por primera vez (PENDIENTE → ATENDIDA)
        if (nuevoEstado == Boleta.EstadoBoleta.ATENDIDA
                && estadoAnterior == Boleta.EstadoBoleta.PENDIENTE) {

            descontarStock(boleta);
        }

        // Caso 2: Se devuelve al inventario (ATENDIDA → PENDIENTE o ATENDIDA → CANCELADA)
        else if (estadoAnterior == Boleta.EstadoBoleta.ATENDIDA
                && (nuevoEstado == Boleta.EstadoBoleta.PENDIENTE || nuevoEstado == Boleta.EstadoBoleta.CANCELADA)) {

            devolverStock(boleta);
        }

        // Caso 3: Se atiende nuevamente después de haber sido cancelada o devuelta (CANCELADA/PENDIENTE → ATENDIDA)
        else if (nuevoEstado == Boleta.EstadoBoleta.ATENDIDA
                && (estadoAnterior == Boleta.EstadoBoleta.CANCELADA || estadoAnterior == Boleta.EstadoBoleta.PENDIENTE)) {

            // Si venía de CANCELADA, el stock ya estaba devuelto → descontamos de nuevo
            descontarStock(boleta);
        }

        // Otros casos (PENDIENTE → CANCELADA, CANCELADA → PENDIENTE, etc.): no tocar stock

        // Actualizar estado
        boleta.setEstado(nuevoEstado);
        Boleta boletaGuardada = boletaRepository.save(boleta);
        BoletaResponse response = toResponse(boletaGuardada);

        // Generar PDF solo al pasar a ATENDIDA por primera vez
        if (nuevoEstado == Boleta.EstadoBoleta.ATENDIDA && estadoAnterior == Boleta.EstadoBoleta.PENDIENTE) {
            try {
                byte[] pdfBytes = pdfService.generarFacturaPdf(response);
                System.out.println("PDF generado para boleta " + boletaGuardada.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return response;
    }

    // === MÉTODOS AUXILIARES PARA REUTILIZAR LÓGICA ===
    private void descontarStock(Boleta boleta) {
        for (BoletaDetalle detalle : boleta.getDetalles()) {
            ProductoVariante variante = detalle.getVariante();
            int stockActual = variante.getStock();

            if (stockActual < detalle.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para '" + variante.getProducto().getNombre() +
                                "' (SKU: " + variante.getSku() + "). Disponible: " + stockActual +
                                ", requerido: " + detalle.getCantidad());
            }

            variante.setStock(stockActual - detalle.getCantidad());
            varianteRepository.save(variante);
        }
    }

    private void devolverStock(Boleta boleta) {
        for (BoletaDetalle detalle : boleta.getDetalles()) {
            ProductoVariante variante = detalle.getVariante();
            variante.setStock(variante.getStock() + detalle.getCantidad());
            varianteRepository.save(variante);
        }
    }
    // ───────────────────────────────────────────────────────────────
    // Mapeadores DTO
    // ───────────────────────────────────────────────────────────────

    private BoletaResponse toResponse(Boleta boleta) {
        BoletaResponse response = new BoletaResponse();
        response.setId(boleta.getId());
        response.setSessionId(boleta.getSessionId());
        response.setTiendaId(boleta.getTienda().getId());
        response.setTotal(boleta.getTotal());
        response.setFecha(boleta.getFecha());
        response.setEstado(boleta.getEstado().name());
        response.setTiendaNombre(boleta.getTienda().getNombre());

        if (boleta.getUser() != null) {
            response.setUserId(boleta.getUser().getId());
        }

        // ── Nuevos campos agregados ────────────────────────────────
        response.setCompradorNombre(boleta.getCompradorNombre());
        response.setCompradorEmail(boleta.getCompradorEmail());
        response.setCompradorTelefono(boleta.getCompradorTelefono());

        response.setDireccionEnvio(boleta.getDireccionEnvio());
        response.setReferenciaEnvio(boleta.getReferenciaEnvio());
        response.setDistrito(boleta.getDistrito());
        response.setProvincia(boleta.getProvincia());
        response.setDepartamento(boleta.getDepartamento());
        response.setCodigoPostal(boleta.getCodigoPostal());

        if (boleta.getTipoEntrega() != null) {
            response.setTipoEntrega(boleta.getTipoEntrega().name());
        }

        response.setDetalles(
                boleta.getDetalles().stream()
                        .map(this::toDetalleResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }
    private BoletaDetalleResponse toDetalleResponse(BoletaDetalle detalle) {
        BoletaDetalleResponse dto = new BoletaDetalleResponse();
        dto.setId(detalle.getId());
        dto.setVarianteId(detalle.getVariante().getId());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());

        ProductoVariante variante = detalle.getVariante();
        dto.setNombreProducto(variante.getProducto().getNombre());
        dto.setSku(variante.getSku());
        dto.setImagenUrl(variante.getImagenUrl());

        return dto;
    }
}