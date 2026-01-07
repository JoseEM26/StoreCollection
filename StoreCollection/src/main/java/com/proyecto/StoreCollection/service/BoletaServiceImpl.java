package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.Exceptions.MissingEmailConfigException;
import com.proyecto.StoreCollection.dto.request.BoletaAdminRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import com.proyecto.StoreCollection.tenant.TenantContext;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        boleta.setCompradorEmail(request.getCompradorEmail());
        boleta.setCompradorTelefono(request.getCompradorTelefono());

        boleta.setDetalles(detalles);
        detalles.forEach(d -> d.setBoleta(boleta));

        Boleta boletaGuardada = boletaRepository.save(boleta);

        descontarStock(boletaGuardada);
        sendEmailNotifications(boletaGuardada);

        return toResponse(boletaGuardada);
    }
// ===================== EMAILS PARA VENTA ADMINISTRATIVA =====================

    private void sendEmailNotifications(Boleta boleta) {
        Tienda tienda = boleta.getTienda();

        // Creamos el sender dinámico con las credenciales de la tienda
        JavaMailSender sender = createMailSenderForTienda(tienda);

        String ownerEmail = tienda.getUser().getEmail();
        String customerEmail = boleta.getCompradorEmail();

        // Siempre notificamos al dueño
        sendToOwnerAdmin(boleta, ownerEmail, sender);

        // Notificamos al cliente solo si tiene un email válido
        if (customerEmail != null && !customerEmail.trim().isEmpty() && customerEmail.contains("@")) {
            sendToCustomerAdmin(boleta, customerEmail, sender);
        }
    }

    private JavaMailSender createMailSenderForTienda(Tienda tienda) {
        if (tienda.getEmailRemitente() == null || tienda.getEmailAppPassword() == null ||
                tienda.getEmailRemitente().trim().isEmpty() || tienda.getEmailAppPassword().trim().isEmpty()) {

            throw new MissingEmailConfigException(
                    "La tienda '" + tienda.getNombre() +
                            "' no tiene configurado correo remitente y contraseña de aplicación. " +
                            "Configúralos en la edición de tienda para enviar notificaciones por correo."
            );
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(tienda.getEmailRemitente());
        mailSender.setPassword(tienda.getEmailAppPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return mailSender;
    }

    private void sendToOwnerAdmin(Boleta boleta, String toEmail, JavaMailSender sender) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            Tienda tienda = boleta.getTienda();
            String fromEmail = tienda.getEmailRemitente() != null && !tienda.getEmailRemitente().isBlank()
                    ? tienda.getEmailRemitente()
                    : tienda.getUser().getEmail();

            helper.setFrom(fromEmail, tienda.getNombre());
            helper.setTo(toEmail);
            helper.setSubject("VENTA DIRECTA REGISTRADA #" + boleta.getId() + " - " + tienda.getNombre());

            String html = buildEmailHtmlForOwnerAdmin(boleta);
            helper.setText(html, true);

            sender.send(msg);
        } catch (Exception e) {
            System.out.println("Error enviando email al dueño para boleta ad");
        }
    }

    private void sendToCustomerAdmin(Boleta boleta, String toEmail, JavaMailSender sender) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            Tienda tienda = boleta.getTienda();
            String fromEmail = tienda.getEmailRemitente() != null && !tienda.getEmailRemitente().isBlank()
                    ? tienda.getEmailRemitente()
                    : tienda.getUser().getEmail();

            helper.setFrom(fromEmail, tienda.getNombre());
            helper.setTo(toEmail);
            helper.setSubject("¡Gracias por tu compra en " + tienda.getNombre() + "! Comprobante #" + boleta.getId());

            String html = buildEmailHtmlForCustomerAdmin(boleta);
            helper.setText(html, true);

            sender.send(msg);
        } catch (Exception e) {
            System.out.println("Error enviando email al cliente para boleta admin ");

        }
    }

    private String buildEmailHtmlForOwnerAdmin(Boleta boleta) {
        StringBuilder sb = new StringBuilder(4096);

        sb.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:0;background:#f8fafc;color:#1e293b;}")
                .append(".container{max-width:640px;margin:30px auto;background:white;border-radius:12px;box-shadow:0 10px 25px rgba(0,0,0,0.1);overflow:hidden;}")
                .append(".header{background:linear-gradient(135deg,#f59e0b,#f97316);color:white;padding:32px;text-align:center;}")
                .append(".header h1{margin:0;font-size:28px;}")
                .append(".content{padding:32px;}")
                .append("table{width:100%;border-collapse:collapse;margin:24px 0;font-size:15px;}")
                .append("th{background:#fff7ed;text-align:left;padding:14px 12px;color:#78350f;font-weight:600;}")
                .append("td{padding:14px 12px;border-bottom:1px solid #fed7aa;}")
                .append(".text-right{text-align:right;}.total{background:#fff7ed;padding:20px;text-align:right;font-size:22px;font-weight:bold;color:#78350f;border-top:3px solid #f97316;}")
                .append(".badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:bold;background:#fef3c7;color:#92400e;}")
                .append("</style></head><body><div class=\"container\">")
                .append("<div class=\"header\"><h1>¡Venta Directa Registrada!</h1>")
                .append("<p style=\"margin:8px 0 0;font-size:18px;\">Comprobante #").append(boleta.getId()).append("</p></div>")
                .append("<div class=\"content\">")
                .append("<p><strong>Tipo:</strong> <span class=\"badge\">Venta en tienda / directa</span></p>")
                .append("<p><strong>Fecha:</strong> ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>")
                .append("<p><strong>Cliente:</strong> ").append(escapeHtml(boleta.getCompradorNombre())).append("</p>")
                .append("<p><strong>Teléfono:</strong> ").append(escapeHtml(orDash(boleta.getCompradorTelefono()))).append("</p>");

        if (boleta.getCompradorEmail() != null) {
            sb.append("<p><strong>Email:</strong> ").append(escapeHtml(boleta.getCompradorEmail())).append("</p>");
        }

        sb.append("<h3 style=\"border-bottom:2px solid #fed7aa;padding-bottom:8px;margin-top:32px;\">Productos vendidos</h3>")
                .append(buildProductsTableEnhanced(boleta))
                .append("<div class=\"total\">Total: S/ ").append(formatPrice(boleta.getTotal())).append("</div>")
                .append("</div></div></body></html>");

        return sb.toString();
    }

    private String buildEmailHtmlForCustomerAdmin(Boleta boleta) {
        StringBuilder sb = new StringBuilder(4096);

        sb.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
                .append("<style>")
                .append("body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:0;background:#fefce8;color:#854d0e;}")
                .append(".container{max-width:640px;margin:30px auto;background:white;border-radius:12px;box-shadow:0 10px 25px rgba(0,0,0,0.1);overflow:hidden;}")
                .append(".header{background:linear-gradient(135deg,#fbbf24,#f59e0b);color:white;padding:40px 32px;text-align:center;}")
                .append(".header h1{margin:0;font-size:30px;}")
                .append(".content{padding:32px;color:#1e293b;}")
                .append("table{width:100%;border-collapse:collapse;margin:24px 0;font-size:15px;}")
                .append("th{background:#fffbeb;text-align:left;padding:14px 12px;color:#854d0e;font-weight:600;}")
                .append("td{padding:14px 12px;border-bottom:1px solid #fde68a;}")
                .append(".text-right{text-align:right;}.total{background:#fffbeb;padding:20px;text-align:right;font-size:22px;font-weight:bold;color:#854d0e;border-top:3px solid #f59e0b;}")
                .append("</style></head><body><div class=\"container\">")
                .append("<div class=\"header\"><h1>¡Gracias por tu compra!</h1>")
                .append("<p style=\"margin:10px 0 0;font-size:19px;\">Comprobante #").append(boleta.getId()).append("</p></div>")
                .append("<div class=\"content\">")
                .append("<h2 style=\"color:#854d0e;\">¡Hola ").append(escapeHtml(boleta.getCompradorNombre())).append("! 🎉</h2>")
                .append("<p>Tu compra en <strong>").append(escapeHtml(boleta.getTienda().getNombre())).append("</strong> ha sido registrada correctamente.</p>")
                .append("<p>Aquí tienes el detalle de lo que adquiriste:</p>")
                .append(buildProductsTableEnhanced(boleta))
                .append("<div class=\"total\">Total pagado: S/ ").append(formatPrice(boleta.getTotal())).append("</div>")
                .append("<p style=\"margin-top:32px;\">¡Gracias por elegirnos! Si necesitas ayuda, contáctanos cuando quieras.</p>")
                .append("</div></div></body></html>");

        return sb.toString();
    }

    // Reutilizas exactamente los mismos helpers que tienes en CarritoServiceImpl
    private String buildProductsTableEnhanced(Boleta boleta) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><thead><tr>")
                .append("<th>Producto</th>")
                .append("<th class=\"text-center\">Cant.</th>")
                .append("<th class=\"text-right\">Precio</th>")
                .append("<th class=\"text-right\">Subtotal</th>")
                .append("</tr></thead><tbody>");

        for (BoletaDetalle d : boleta.getDetalles()) {
            ProductoVariante v = d.getVariante();
            String nombre = escapeHtml(v.getProducto().getNombre());
            String variantes = "";
            if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
                StringBuilder vars = new StringBuilder("<br><small style=\"color:#64748b;\">");
                for (AtributoValor av : v.getAtributos()) {
                    String attrName = av.getAtributo() != null ? av.getAtributo().getNombre() : "";
                    vars.append(attrName).append(": ").append(escapeHtml(av.getValor())).append(" • ");
                }
                variantes = vars.substring(0, vars.length() - 3) + "</small>";
            }

            sb.append("<tr>")
                    .append("<td>").append(nombre).append(variantes).append("</td>")
                    .append("<td class=\"text-center\">").append(d.getCantidad()).append("</td>")
                    .append("<td class=\"text-right\">S/ ").append(formatPrice(d.getPrecioUnitario())).append("</td>")
                    .append("<td class=\"text-right\">S/ ").append(formatPrice(d.getSubtotal())).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String orDash(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
    }



    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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