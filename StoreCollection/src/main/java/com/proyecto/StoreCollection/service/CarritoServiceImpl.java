package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarritoServiceImpl implements CarritoService {

    private final JavaMailSender mailSender;
    private final CarritoRepository repository;
    private final ProductoVarianteRepository varianteRepository;
    private final BoletaRepository boletaRepository;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    // ===================== CARRITO =====================

    @Override
    @Transactional(readOnly = true)
    public List<CarritoResponse> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse findById(Integer id) {
        Carrito carrito = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));
        return toResponse(carrito);
    }

    @Override
    public CarritoResponse save(CarritoRequest request) {
        return save(request, null);
    }

    @Override
    public CarritoResponse save(CarritoRequest request, Integer id) {
        Carrito carrito = (id == null)
                ? new Carrito()
                : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));

        carrito.setSessionId(request.getSessionId());
        carrito.setCantidad(request.getCantidad());

        if (request.getVarianteId() != null) {
            ProductoVariante variante = varianteRepository.findById(request.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + request.getVarianteId()));
            carrito.setVariante(variante);
        }

        return toResponse(repository.save(carrito));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public void limpiarCarrito(String sessionId) {
        repository.deleteBySessionId(sessionId);
    }

    // ===================== CHECKOUT ONLINE =====================

    @Override
    @Transactional
    public BoletaResponse checkoutOnline(BoletaRequest request) {
        List<Carrito> items = repository.findBySessionIdWithDetails(request.getSessionId());
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío");
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + request.getTiendaId()));

        BigDecimal total = BigDecimal.ZERO;
        List<BoletaDetalle> detalles = new ArrayList<>();

        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();
            if (variante == null) {
                throw new IllegalStateException("Variante no encontrada en carrito");
            }

            if (variante.getStock() < item.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para " + variante.getProducto().getNombre() +
                                " (SKU: " + variante.getSku() + "). Disponible: " + variante.getStock() +
                                ", solicitado: " + item.getCantidad());
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
        boleta.setSessionId(request.getSessionId());
        boleta.setTienda(tienda);
        boleta.setTotal(total);
        boleta.setEstado(Boleta.EstadoBoleta.PENDIENTE);

        if (request.getUserId() != null) {
            Usuario user = usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boleta.setUser(user);
        }

        boleta.setCompradorNombre(request.getCompradorNombre());
        boleta.setCompradorEmail(request.getCompradorEmail());
        boleta.setCompradorTelefono(request.getCompradorTelefono());
        boleta.setDireccionEnvio(request.getDireccionEnvio());
        boleta.setReferenciaEnvio(request.getReferenciaEnvio());
        boleta.setDistrito(request.getDistrito());
        boleta.setProvincia(request.getProvincia());
        boleta.setDepartamento(request.getDepartamento());
        boleta.setCodigoPostal(request.getCodigoPostal());
        boleta.setTipoEntrega(request.getTipoEntrega());

        boleta.setDetalles(detalles);
        detalles.forEach(d -> d.setBoleta(boleta));

        Boleta boletaGuardada = boletaRepository.save(boleta);
        limpiarCarrito(request.getSessionId());

        // Enviamos notificaciones por email
        sendEmailNotifications(boletaGuardada);

        return toBoletaResponse(boletaGuardada);
    }

    // ===================== EMAILS =====================

    private void sendEmailNotifications(Boleta boleta) {
        String ownerEmail = boleta.getTienda().getUser().getEmail();
        String customerEmail = boleta.getCompradorEmail();

        // Siempre enviamos al dueño
        sendToOwner(boleta, ownerEmail);

        // Enviamos al cliente solo si hay email válido
        if (customerEmail != null && !customerEmail.trim().isEmpty() && customerEmail.contains("@")) {
            sendToCustomer(boleta, customerEmail);
        }
    }

    private void sendToOwner(Boleta boleta, String toEmail) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom("StoreCollection <joseangelespinozamorales@hotmail.com>");
            helper.setTo(toEmail);
            helper.setSubject("¡NUEVO PEDIDO! #" + boleta.getId() + " - " + boleta.getTienda().getNombre());

            String html = buildEmailHtmlForOwner(boleta);
            helper.setText(html, true);

            mailSender.send(msg);
            System.out.println("Email enviado al dueño: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando email al dueño (" + toEmail + "): " + e.getMessage());
        }
    }

    private void sendToCustomer(Boleta boleta, String toEmail) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom("StoreCollection <joseangelespinozamorales@hotmail.com>");
            helper.setTo(toEmail);
            helper.setSubject("¡Gracias por tu compra! Pedido #" + boleta.getId());

            String html = buildEmailHtmlForCustomer(boleta);
            helper.setText(html, true);

            mailSender.send(msg);
            System.out.println("Email de confirmación enviado al cliente: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación al cliente (" + toEmail + "): " + e.getMessage());
        }
    }

    private String buildEmailHtmlForOwner(Boleta boleta) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"es\">")
                .append("<head>")
                .append("  <meta charset=\"UTF-8\">")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("  <title>Nuevo Pedido #").append(boleta.getId()).append("</title>")
                .append("  <style>")
                .append("    body { font-family: Arial, Helvetica, sans-serif; margin:0; padding:0; background:#f6f9fc; color:#333; }")
                .append("    .container { max-width:600px; margin:40px auto; background:white; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.08); overflow:hidden; }")
                .append("    .header { background:#6366f1; color:white; padding:24px 32px; text-align:center; }")
                .append("    .content { padding:32px; }")
                .append("    table { width:100%; border-collapse:collapse; margin:20px 0; }")
                .append("    th, td { padding:12px; text-align:left; border-bottom:1px solid #e5e7eb; }")
                .append("    th { background:#f3f4f6; }")
                .append("    .total { text-align:right; font-size:20px; font-weight:bold; margin:24px 0; }")
                .append("    .footer { background:#f9fafb; padding:24px; text-align:center; font-size:13px; color:#6b7280; }")
                .append("  </style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("  <div class=\"header\"><h1>Nuevo Pedido #").append(boleta.getId()).append("</h1></div>")
                .append("  <div class=\"content\">")
                .append("    <p><strong>Tienda:</strong> ").append(escapeHtml(boleta.getTienda().getNombre())).append("</p>")
                .append("    <p><strong>Fecha:</strong> ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>")
                .append("    <p><strong>Cliente:</strong> ").append(escapeHtml(boleta.getCompradorNombre())).append("</p>")
                .append("    <p><strong>Email:</strong> ").append(escapeHtml(boleta.getCompradorEmail())).append("</p>")
                .append("    <p><strong>Teléfono:</strong> ").append(escapeHtml(boleta.getCompradorTelefono() != null ? boleta.getCompradorTelefono() : "-")).append("</p>")
                .append("    <p><strong>Dirección:</strong> ").append(escapeHtml(boleta.getDireccionCompleta())).append("</p>")
                .append("    <h3>Productos</h3>")
                .append(buildProductsTable(boleta))
                .append("    <div class=\"total\">Total: S/ ").append(boleta.getTotal()).append("</div>")
                .append("  </div>")
                .append("  <div class=\"footer\">")
                .append("    <p>StoreCollection © ").append(LocalDate.now().getYear()).append(" | Mensaje automático</p>")
                .append("  </div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return sb.toString();
    }

    private String buildEmailHtmlForCustomer(Boleta boleta) {
        StringBuilder sb = new StringBuilder(2048);

        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"es\">")
                .append("<head>")
                .append("  <meta charset=\"UTF-8\">")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("  <title>¡Gracias por tu compra! #").append(boleta.getId()).append("</title>")
                .append("  <style>")
                .append("    body { font-family: Arial, Helvetica, sans-serif; margin:0; padding:0; background:#f6f9fc; color:#333; }")
                .append("    .container { max-width:600px; margin:40px auto; background:white; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.08); overflow:hidden; }")
                .append("    .header { background:#10b981; color:white; padding:32px 24px; text-align:center; }")
                .append("    .content { padding:32px; }")
                .append("    table { width:100%; border-collapse:collapse; margin:20px 0; }")
                .append("    th, td { padding:12px; text-align:left; border-bottom:1px solid #e5e7eb; }")
                .append("    th { background:#f3f4f6; }")
                .append("    .total { text-align:right; font-size:20px; font-weight:bold; margin:24px 0; }")
                .append("    .footer { background:#f9fafb; padding:24px; text-align:center; font-size:13px; color:#6b7280; }")
                .append("  </style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("  <div class=\"header\"><h2>¡Gracias por tu compra!</h2></div>")
                .append("  <div class=\"content\">")
                .append("    <h3>Hola ").append(escapeHtml(boleta.getCompradorNombre())).append("</h3>")
                .append("    <p>Hemos recibido correctamente tu pedido <strong>#").append(boleta.getId()).append("</strong></p>")
                .append("    <p>Te contactaremos muy pronto por WhatsApp o correo para coordinar la entrega.</p>")
                .append("    <h3>Resumen de tu pedido</h3>")
                .append(buildProductsTable(boleta))
                .append("    <div class=\"total\">Total: S/ ").append(boleta.getTotal()).append("</div>")
                .append("    <p style=\"margin-top:32px;\">¡Esperamos que disfrutes mucho tus productos! 🛍️</p>")
                .append("  </div>")
                .append("  <div class=\"footer\">")
                .append("    <p>").append(escapeHtml(boleta.getTienda().getNombre())).append(" © ").append(LocalDate.now().getYear()).append("</p>")
                .append("    <p>Mensaje automático – no responder</p>")
                .append("  </div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return sb.toString();
    }

    private String buildProductsTable(Boleta boleta) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>")
                .append("<thead><tr>")
                .append("<th>Producto</th>")
                .append("<th style=\"text-align:center;\">Cant.</th>")
                .append("<th style=\"text-align:right;\">Precio</th>")
                .append("<th style=\"text-align:right;\">Subtotal</th>")
                .append("</tr></thead>")
                .append("<tbody>");

        for (BoletaDetalle d : boleta.getDetalles()) {
            String nombre = escapeHtml(d.getVariante().getProducto().getNombre());
            sb.append("<tr>")
                    .append("<td>").append(nombre).append("</td>")
                    .append("<td style=\"text-align:center;\">").append(d.getCantidad()).append("</td>")
                    .append("<td style=\"text-align:right;\">S/ ").append(d.getPrecioUnitario()).append("</td>")
                    .append("<td style=\"text-align:right;\">S/ ").append(d.getSubtotal()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table>");
        return sb.toString();
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ===================== CHECKOUT WHATSAPP =====================

    @Override
    @Transactional(readOnly = true)
    public String checkoutWhatsapp(BoletaRequest request) {
        List<Carrito> items = repository.findBySessionIdWithDetails(request.getSessionId());

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío o la sesión no existe");
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + request.getTiendaId()));

        String numeroWhats = normalizeWhatsappNumber(tienda.getWhatsapp());
        if (numeroWhats == null || numeroWhats.length() < 9) {
            throw new IllegalStateException("Número de WhatsApp inválido: " + tienda.getWhatsapp());
        }

        StringBuilder msg = new StringBuilder();
        msg.append("🛒 *¡NUEVO PEDIDO EN ").append(tienda.getNombre().toUpperCase()).append("!*\n\n");

        BigDecimal total = BigDecimal.ZERO;
        int itemNumber = 1;
        int validItems = 0;

        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();
            if (variante == null || variante.getProducto() == null) {
                continue;
            }

            validItems++;
            Producto producto = variante.getProducto();

            msg.append("*").append(itemNumber).append(".* ")
                    .append(producto.getNombre())
                    .append(" × ").append(item.getCantidad()).append(" und.\n");

            BigDecimal precioUnit = variante.getPrecio();
            BigDecimal subtotal = precioUnit.multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            msg.append("   💵 Precio: S/ ").append(formatPrice(precioUnit))
                    .append("   → Subtotal: S/ ").append(formatPrice(subtotal)).append("\n");

            if (variante.getSku() != null && !variante.getSku().isBlank()) {
                msg.append("   🏷️ SKU: ").append(variante.getSku()).append("\n");
            }

            if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                msg.append("   🎨 *Variantes:*\n");
                for (AtributoValor av : variante.getAtributos()) {
                    String nombreAttr = av.getAtributo() != null ? av.getAtributo().getNombre() : "Atributo";
                    msg.append("      • ").append(nombreAttr).append(": *").append(av.getValor()).append("*\n");
                }
            }

            if (variante.getImagenUrl() != null && !variante.getImagenUrl().isBlank()) {
                msg.append("   📸 ").append(shortenUrl(variante.getImagenUrl())).append("\n");
            }

            msg.append("\n");
            itemNumber++;
        }

        if (validItems == 0) {
            throw new IllegalStateException("No hay ítems válidos en el carrito");
        }

        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("📊 *RESUMEN DEL PEDIDO*\n");
        msg.append("Items: ").append(validItems).append("\n");
        msg.append("💰 *TOTAL: S/ ").append(formatPrice(total)).append("*\n");
        msg.append("📅 ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE")))).append("\n\n");

        msg.append("👤 *DATOS PARA COORDINAR:*\n");
        msg.append("• Nombre: ______________________\n");
        msg.append("• Teléfono: ____________________\n");
        msg.append("• Dirección: ___________________\n\n");
        msg.append("✅ Responde para confirmar el pedido 🙌");

        String encodedMessage;
        try {
            encodedMessage = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            encodedMessage = URLEncoder.encode(msg.toString().replaceAll("[^\\p{ASCII}]", "?"), StandardCharsets.UTF_8);
        }

        limpiarCarrito(request.getSessionId());

        return "https://wa.me/" + numeroWhats + "?text=" + encodedMessage;
    }

    // ===================== MÉTODOS AUXILIARES =====================

    private String normalizeWhatsappNumber(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("00")) cleaned = cleaned.substring(2);
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);
        if (cleaned.length() == 9 && !cleaned.startsWith("51")) cleaned = "51" + cleaned;
        return (cleaned.length() >= 10 && cleaned.length() <= 14) ? cleaned : null;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", price);
    }

    private String shortenUrl(String url) {
        return (url != null && url.length() > 50) ? url.substring(0, 47) + "..." : url;
    }

    private CarritoResponse toResponse(Carrito c) {
        CarritoResponse dto = new CarritoResponse();
        dto.setId(c.getId());
        dto.setSessionId(c.getSessionId());
        dto.setCantidad(c.getCantidad());

        if (c.getVariante() != null) {
            ProductoVariante v = c.getVariante();
            dto.setVarianteId(v.getId());
            dto.setNombreProducto(v.getProducto().getNombre());
            dto.setSku(v.getSku());
            dto.setPrecio(v.getPrecio().doubleValue());
            dto.setImagenUrl(v.getImagenUrl());

            List<AtributoResponse> atributos = v.getAtributos().stream()
                    .map(av -> {
                        AtributoResponse attr = new AtributoResponse();
                        attr.setId(av.getId());
                        attr.setNombre(av.getAtributo() != null ? av.getAtributo().getNombre() : "");
                        attr.setValor(av.getValor());
                        attr.setTiendaId(av.getAtributo() != null && av.getAtributo().getTienda() != null ?
                                av.getAtributo().getTienda().getId() : null);
                        return attr;
                    })
                    .collect(Collectors.toList());

            dto.setAtributos(atributos.isEmpty() ? null : atributos);
        }
        return dto;
    }

    private BoletaResponse toBoletaResponse(Boleta boleta) {
        BoletaResponse response = new BoletaResponse();
        response.setId(boleta.getId());
        response.setSessionId(boleta.getSessionId());
        response.setTiendaId(boleta.getTienda().getId());
        response.setTotal(boleta.getTotal());
        response.setFecha(boleta.getFecha());
        response.setEstado(boleta.getEstado().name());

        if (boleta.getUser() != null) {
            response.setUserId(boleta.getUser().getId());
        }

        response.setCompradorNombre(boleta.getCompradorNombre());
        response.setCompradorEmail(boleta.getCompradorEmail());
        response.setCompradorTelefono(boleta.getCompradorTelefono());
        response.setDireccionEnvio(boleta.getDireccionEnvio());
        response.setReferenciaEnvio(boleta.getReferenciaEnvio());
        response.setDistrito(boleta.getDistrito());
        response.setProvincia(boleta.getProvincia());
        response.setDepartamento(boleta.getDepartamento());
        response.setCodigoPostal(boleta.getCodigoPostal());
        response.setTipoEntrega(boleta.getTipoEntrega() != null ? boleta.getTipoEntrega().name() : null);

        response.setDetalles(
                boleta.getDetalles().stream()
                        .map(this::toBoletaDetalleResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private BoletaDetalleResponse toBoletaDetalleResponse(BoletaDetalle detalle) {
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

        List<AtributoResponse> atributos = variante.getAtributos().stream()
                .map(av -> {
                    AtributoResponse attr = new AtributoResponse();
                    attr.setId(av.getId());
                    attr.setNombre(av.getAtributo() != null ? av.getAtributo().getNombre() : "");
                    attr.setValor(av.getValor());
                    attr.setTiendaId(av.getAtributo() != null && av.getAtributo().getTienda() != null ?
                            av.getAtributo().getTienda().getId() : null);
                    return attr;
                })
                .collect(Collectors.toList());

        dto.setAtributos(atributos.isEmpty() ? null : atributos);
        return dto;
    }
}