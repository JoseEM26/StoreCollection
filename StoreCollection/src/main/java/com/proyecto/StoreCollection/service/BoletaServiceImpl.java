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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


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
    // ===================== ENVÍO DE CONFIRMACIÓN AL CLIENTE POR WHATSAPP =====================
    @Override
    @Transactional(readOnly = true)
    public String generarMensajeConfirmacionCliente(Integer boletaId) {
        Boleta boleta = boletaRepository.findById(boletaId)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + boletaId));

        verificarPermisosSobreBoleta(boleta);

        // Validamos que tenga teléfono del cliente
        if (boleta.getCompradorTelefono() == null || boleta.getCompradorTelefono().trim().isEmpty()) {
            throw new IllegalStateException("El cliente no tiene número de teléfono registrado. No se puede enviar por WhatsApp.");
        }

        String telefonoCliente = normalizeWhatsappNumber(boleta.getCompradorTelefono());
        if (telefonoCliente == null || telefonoCliente.length() < 9) {
            throw new IllegalStateException("Número de teléfono inválido: " + boleta.getCompradorTelefono());
        }

        StringBuilder msg = new StringBuilder();

        msg.append("¡Hola *").append(escapeMarkdown(boleta.getCompradorNombre())).append("*!\n");
        msg.append("Gracias por tu compra en *").append(escapeMarkdown(boleta.getTienda().getNombre())).append("* 🛍️\n\n");

        msg.append("📋 *RESUMEN DE TU PEDIDO #").append(boleta.getId()).append("*\n");
        msg.append("📅 Fecha: ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        // Productos
        msg.append("🛒 *PRODUCTOS*\n");
        int item = 1;
        for (BoletaDetalle d : boleta.getDetalles()) {
            ProductoVariante v = d.getVariante();
            msg.append(item++).append(". ").append(escapeMarkdown(v.getProducto().getNombre()));
            if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
                msg.append(" (");
                msg.append(v.getAtributos().stream()
                        .map(av -> {
                            String attrName = av.getAtributo() != null ? av.getAtributo().getNombre() : "";
                            return attrName + ": " + escapeMarkdown(av.getValor());
                        })
                        .collect(Collectors.joining(", ")));
                msg.append(")");
            }
            msg.append("\n   Cantidad: ").append(d.getCantidad())
                    .append(" × S/ ").append(formatPrice(d.getPrecioUnitario()))
                    .append(" = S/ ").append(formatPrice(d.getSubtotal())).append("\n");
        }

        msg.append("\n💰 *TOTAL PAGADO: S/ ").append(formatPrice(boleta.getTotal())).append("*\n\n");

        // Detalles de entrega
        msg.append("🚚 *DETALLES DE ENTREGA*\n");

        if (boleta.getTipoEntrega() != null) {
            String tipoTexto = switch (boleta.getTipoEntrega()) {
                case DOMICILIO -> "Envío a domicilio";
                case RECOGIDA_EN_TIENDA -> "Recojo en tienda";
                case AGENCIA -> "Envío por agencia";
            };
            msg.append("• Tipo: ").append(tipoTexto).append("\n");
        }

        if (boleta.getDireccionEnvio() != null && !boleta.getDireccionEnvio().trim().isEmpty()) {
            msg.append("• Dirección: ").append(escapeMarkdown(boleta.getDireccionEnvio().trim()));
            if (boleta.getReferenciaEnvio() != null && !boleta.getReferenciaEnvio().trim().isEmpty()) {
                msg.append(" - ").append(escapeMarkdown(boleta.getReferenciaEnvio().trim()));
            }
            msg.append("\n");
            if (boleta.getDistrito() != null) {
                msg.append("• Distrito: ").append(escapeMarkdown(boleta.getDistrito()));
                if (boleta.getDepartamento() != null) {
                    msg.append(" - ").append(escapeMarkdown(boleta.getDepartamento()));
                }
                msg.append("\n");
            }
        } else {
            msg.append("• Dirección: Por confirmar / Recojo en tienda / Agencia\n");
        }

        msg.append("\n📞 Te contactaremos pronto para coordinar la entrega.\n");
        msg.append("Si todo está correcto, responde *OK* o avísanos cualquier cambio.\n\n");

        msg.append("¡Gracias por tu preferencia! Estamos preparando todo con mucho cariño ❤️\n");
        msg.append(boleta.getTienda().getNombre());

        String encodedMessage = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);

        return "https://wa.me/" + telefonoCliente + "?text=" + encodedMessage;
    }

// ===================== MÉTODOS AUXILIARES (agregar al final de la clase) =====================

    private String normalizeWhatsappNumber(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("00")) cleaned = cleaned.substring(2);
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);
        if (cleaned.length() == 9 && !cleaned.startsWith("51")) cleaned = "51" + cleaned;
        return (cleaned.length() >= 10 && cleaned.length() <= 14) ? cleaned : null;
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", price);
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