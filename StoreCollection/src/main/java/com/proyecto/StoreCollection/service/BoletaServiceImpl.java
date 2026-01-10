package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.Exceptions.MissingEmailConfigException;
import com.proyecto.StoreCollection.dto.request.BoletaAdminRequest;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class BoletaServiceImpl implements BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoVarianteRepository varianteRepository;
    private final PdfService pdfService;
    private final TiendaRepository tiendaRepository;
    private final CarritoService carritoService;
    // ───────────────────────────────────────────────────────────────
    // Listados paginados
    // ───────────────────────────────────────────────────────────────


    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findAll(Pageable pageable) {
        return boletaRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoletaResponse> findByTiendaId(Integer tiendaId, Pageable pageable) {
        return boletaRepository.findByTiendaId(tiendaId, pageable).map(this::toResponse);
    }
    // ===================== ENVÍO DE CONFIRMACIÓN AL CLIENTE POR WHATSAPP =====================
    @Override
    @Transactional(readOnly = true)
    public String generarMensajeConfirmacionCliente(Integer boletaId) {
        Boleta boleta = boletaRepository.findById(boletaId)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + boletaId));

        verificarPermisosSobreBoleta(boleta);

        if (!StringUtils.hasText(boleta.getCompradorNumero())) {
            throw new IllegalStateException("El cliente no tiene número de teléfono registrado");
        }

        String telefonoCliente = normalizeWhatsappNumber(boleta.getCompradorNumero());
        if (telefonoCliente == null) {
            throw new IllegalStateException("Número de teléfono inválido: " + boleta.getCompradorNumero());
        }

        StringBuilder msg = new StringBuilder();

        msg.append("¡Hola *").append(escapeMarkdown(boleta.getCompradorNombre() != null ? boleta.getCompradorNombre() : "Cliente"))
                .append("*!\n")
                .append("Gracias por tu compra en *").append(escapeMarkdown(boleta.getTienda().getNombre())).append("* 🛍️\n\n");

        msg.append("📋 *PEDIDO #").append(boleta.getId()).append("*\n")
                .append("📅 ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        // Productos
        msg.append("🛒 *PRODUCTOS*\n");
        int itemNum = 1;
        for (BoletaDetalle d : boleta.getDetalles()) {
            ProductoVariante v = d.getVariante();
            msg.append(itemNum++).append(". *").append(escapeMarkdown(v.getProducto().getNombre())).append("*\n");

            if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
                msg.append("   Opciones: ")
                        .append(v.getAtributos().stream()
                                .map(av -> escapeMarkdown((av.getAtributo() != null ? av.getAtributo().getNombre() : "") + ": " + av.getValor()))
                                .collect(Collectors.joining(", ")))
                        .append("\n");
            }

            msg.append("   Cantidad: ").append(d.getCantidad())
                    .append(" × S/ ").append(formatPrice(d.getPrecioUnitario()))
                    .append(" = S/ ").append(formatPrice(d.getSubtotal())).append("\n\n");
        }

        msg.append("━━━━━━━━━━━━━━━━━━━\n")
                .append("💰 *TOTAL: S/ ").append(formatPrice(boleta.getTotal())).append("*\n\n");

        // Detalles de entrega
        msg.append("🚚 *DETALLES DE ENTREGA*\n");
        msg.append("Tipo: ").append(boleta.getTipoEntrega() != null ? boleta.getTipoEntrega().name() : "No especificado").append("\n");

        if (StringUtils.hasText(boleta.getDireccionEnvio())) {
            msg.append("Dirección: ").append(escapeMarkdown(boleta.getDireccionEnvio())).append("\n");
            if (StringUtils.hasText(boleta.getReferenciaEnvio())) {
                msg.append("Referencia: ").append(escapeMarkdown(boleta.getReferenciaEnvio())).append("\n");
            }
            if (StringUtils.hasText(boleta.getDistrito())) {
                msg.append("Distrito: ").append(escapeMarkdown(boleta.getDistrito())).append("\n");
            }
            String ubigeo = Stream.of(boleta.getProvincia(), boleta.getDepartamento())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            if (!ubigeo.isEmpty()) {
                msg.append("Ubicación: ").append(ubigeo).append("\n");
            }
        } else {
            msg.append("A coordinar contigo directamente 📲\n");
        }

        msg.append("\nTe contactaremos pronto para coordinar todo.\n");
        msg.append("Si todo está correcto, responde *OK* o dime si necesitas algún cambio.\n\n");

        msg.append("¡Gracias por confiar en nosotros! ❤️\n")
                .append(boleta.getTienda().getNombre());

        String encoded = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
        return "https://wa.me/" + telefonoCliente + "?text=" + encoded;
    }
// ===================== MÉTODOS AUXILIARES (agregar al final de la clase) =====================


    private String normalizeWhatsappNumber(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("00")) cleaned = cleaned.substring(2);
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);
        if (cleaned.length() == 9 && !cleaned.startsWith("51")) cleaned = "51" + cleaned;
        return (cleaned.length() >= 9 && cleaned.length() <= 15) ? cleaned : null;
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
        return price != null ? String.format(Locale.forLanguageTag("es-PE"), "%.2f", price) : "0.00";
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

    @Transactional
    public BoletaResponse crearBoletaAdmin(BoletaAdminRequest request) {
        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null || !tenantId.equals(request.getTiendaId())) {
            throw new AccessDeniedException("No tienes permisos para crear boletas en esta tienda");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto");
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .filter(Tienda::getActivo)
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada o inactiva"));

        BigDecimal total = BigDecimal.ZERO;
        List<BoletaDetalle> detalles = new ArrayList<>();

        for (BoletaAdminRequest.ItemRequest item : request.getItems()) {
            ProductoVariante variante = varianteRepository.findById(item.getVarianteId())
                    .filter(v -> v.isActivo() && v.getProducto().isActivo())
                    .filter(v -> v.getTienda().getId().equals(tenantId))
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada, inactiva o no pertenece a esta tienda"));

            if (variante.getStock() < item.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para " + variante.getProducto().getNombre() +
                                " (disponible: " + variante.getStock() + ")"
                );
            }

            BigDecimal subtotal = variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            BoletaDetalle detalle = new BoletaDetalle();
            detalle.setVariante(variante);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(variante.getPrecio());
            detalle.setSubtotal(subtotal);
            detalles.add(detalle);
        }

        Boleta boleta = new Boleta();
        boleta.setTienda(tienda);
        boleta.setTotal(total);
        boleta.setEstado(Boleta.EstadoBoleta.ATENDIDA);
        boleta.setFecha(LocalDateTime.now());

        boleta.setCompradorNombre(
                StringUtils.hasText(request.getCompradorNombre())
                        ? request.getCompradorNombre().trim()
                        : "Cliente en tienda (venta directa)"
        );
        boleta.setCompradorNumero(request.getCompradorNumero());

        boleta.setDetalles(detalles);
        detalles.forEach(d -> d.setBoleta(boleta));

        Boleta boletaGuardada = boletaRepository.save(boleta);

        descontarStock(boletaGuardada);

        return toResponse(boletaGuardada);
    }
// ===================== EMAILS PARA VENTA ADMINISTRATIVA =====================





    private BoletaResponse toResponse(Boleta boleta) {
        BoletaResponse r = new BoletaResponse();
        r.setId(boleta.getId());
        r.setSessionId(boleta.getSessionId());
        r.setTiendaId(boleta.getTienda().getId());
        r.setTiendaNombre(boleta.getTienda().getNombre());
        r.setTotal(boleta.getTotal());
        r.setFecha(boleta.getFecha());
        r.setEstado(boleta.getEstado().name());

        if (boleta.getUser() != null) {
            r.setUserId(boleta.getUser().getId());
        }

        // Campos del comprador
        r.setCompradorNombre(boleta.getCompradorNombre());
        r.setCompradorNumero(boleta.getCompradorNumero());

        // Campos de dirección y entrega
        r.setDireccionEnvio(boleta.getDireccionEnvio());
        r.setReferenciaEnvio(boleta.getReferenciaEnvio());
        r.setDistrito(boleta.getDistrito());
        r.setProvincia(boleta.getProvincia());
        r.setDepartamento(boleta.getDepartamento());
        r.setCodigoPostal(boleta.getCodigoPostal());
        r.setTipoEntrega(boleta.getTipoEntrega() != null ? boleta.getTipoEntrega().name() : null);

        r.setDetalles(boleta.getDetalles().stream()
                .map(this::toDetalleResponse)
                .collect(Collectors.toList()));

        return r;
    }

    private BoletaDetalleResponse toDetalleResponse(BoletaDetalle detalle) {
        BoletaDetalleResponse dto = new BoletaDetalleResponse();
        dto.setId(detalle.getId());
        dto.setVarianteId(detalle.getVariante().getId());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());

        ProductoVariante v = detalle.getVariante();
        dto.setNombreProducto(v.getProducto().getNombre());
        dto.setSku(v.getSku());
        dto.setImagenUrl(v.getImagenUrl());

        return dto;
    }
}