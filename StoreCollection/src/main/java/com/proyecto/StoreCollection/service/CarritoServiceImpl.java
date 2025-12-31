package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
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
import java.util.Properties;
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
    private JavaMailSender getConfiguredMailSender(Tienda tienda) {
        String authEmail = tienda.getUser().getEmail(); // siempre usamos el email del usuario para autenticación
        String appPassword = tienda.getEmailAppPassword();

        // Si no hay contraseña de app configurada → usamos el mailSender global (por ahora)
        if (appPassword == null || appPassword.trim().isEmpty()) {
            return mailSender; // ← el que inyectaste por Spring (application.yml)
        }

        // Creamos un sender específico para esta tienda
        JavaMailSenderImpl customSender = new JavaMailSenderImpl();
        customSender.setHost("smtp.gmail.com");
        customSender.setPort(587);

        customSender.setUsername(authEmail);
        customSender.setPassword(appPassword);

        Properties props = customSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        // Opcional: props.put("mail.debug", "true"); // para depuración

        return customSender;
    }private void sendToOwner(Boleta boleta, String toEmail) {
        Tienda tienda = boleta.getTienda();
        JavaMailSender sender = getConfiguredMailSender(tienda);

        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            // Prioridad: email_remitente de la tienda > email del usuario
            String fromEmail = tienda.getEmailRemitente() != null && !tienda.getEmailRemitente().trim().isEmpty()
                    ? tienda.getEmailRemitente()
                    : tienda.getUser().getEmail();

            String fromName = tienda.getNombre(); // o "Tienda " + tienda.getNombre()

            helper.setFrom(fromEmail, fromName); // ← formato profesional: "Tienda X <ventas@mitienda.com>"
            helper.setTo(toEmail);
            helper.setSubject("¡NUEVO PEDIDO! #" + boleta.getId() + " - " + tienda.getNombre());

            String html = buildEmailHtmlForOwner(boleta);
            helper.setText(html, true);

            sender.send(msg);
            System.out.println("Email enviado al dueño: " + toEmail + " desde: " + fromEmail);
        } catch (Exception e) {
            System.err.println("Error enviando email al dueño (" + toEmail + "): " + e.getMessage());
        }
    }

    private void sendToCustomer(Boleta boleta, String toEmail) {
        Tienda tienda = boleta.getTienda();
        JavaMailSender sender = getConfiguredMailSender(tienda);

        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            String fromEmail = tienda.getEmailRemitente() != null && !tienda.getEmailRemitente().trim().isEmpty()
                    ? tienda.getEmailRemitente()
                    : tienda.getUser().getEmail();

            String fromName = tienda.getNombre();

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("¡Gracias por tu compra! Pedido #" + boleta.getId() + " - " + tienda.getNombre());

            String html = buildEmailHtmlForCustomer(boleta);
            helper.setText(html, true);

            sender.send(msg);
            System.out.println("Email de confirmación enviado al cliente: " + toEmail + " desde: " + fromEmail);
        } catch (Exception e) {
            System.err.println("Error enviando email de confirmación al cliente (" + toEmail + "): " + e.getMessage());
        }
    }

    private String buildEmailHtmlForOwner(Boleta boleta) {
        StringBuilder sb = new StringBuilder(4096);

        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"es\">")
                .append("<head>")
                .append("  <meta charset=\"UTF-8\">")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("  <title>Nuevo Pedido #").append(boleta.getId()).append("</title>")
                .append("  <style>")
                .append("    body { font-family: 'Segoe UI', Arial, sans-serif; margin:0; padding:0; background:#f8fafc; color:#1e293b; }")
                .append("    .container { max-width:640px; margin:30px auto; background:white; border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,0.1); overflow:hidden; }")
                .append("    .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); color:white; padding:32px; text-align:center; }")
                .append("    .header h1 { margin:0; font-size:28px; }")
                .append("    .content { padding:32px; }")
                .append("    .info-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; margin:24px 0; font-size:15px; }")
                .append("    .info-item strong { color:#475569; }")
                .append("    table { width:100%; border-collapse:collapse; margin:24px 0; font-size:15px; }")
                .append("    th { background:#f1f5f9; text-align:left; padding:14px 12px; color:#475569; font-weight:600; }")
                .append("    td { padding:14px 12px; border-bottom:1px solid #e2e8f0; }")
                .append("    .text-right { text-align:right; }")
                .append("    .text-center { text-align:center; }")
                .append("    .total { background:#f8fafc; padding:20px; text-align:right; font-size:22px; font-weight:bold; color:#1e293b; border-top:3px solid #6366f1; }")
                .append("    .badge { display:inline-block; padding:4px 12px; border-radius:20px; font-size:12px; font-weight:bold; background:#e0e7ff; color:#4338ca; }")
                .append("    .footer { background:#f1f5f9; padding:24px; text-align:center; font-size:13px; color:#64748b; }")
                .append("  </style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("  <div class=\"header\">")
                .append("    <h1>¡Nuevo Pedido Recibido!</h1>")
                .append("    <p style=\"margin:8px 0 0; font-size:18px;\">Pedido #").append(boleta.getId()).append("</p>")
                .append("  </div>")
                .append("  <div class=\"content\">")

                // Información del pedido
                .append("    <div class=\"info-grid\">")
                .append("      <div><strong>Tienda:</strong> ").append(escapeHtml(boleta.getTienda().getNombre())).append("</div>")
                .append("      <div><strong>Fecha:</strong> ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</div>")
                .append("      <div><strong>Cliente:</strong> ").append(escapeHtml(boleta.getCompradorNombre())).append("</div>")
                .append("      <div><strong>Email:</strong> ").append(escapeHtml(boleta.getCompradorEmail())).append("</div>")
                .append("      <div><strong>Teléfono:</strong> ").append(escapeHtml(orDash(boleta.getCompradorTelefono()))).append("</div>");

        // Tipo de entrega
        if (boleta.getTipoEntrega() != null) {
            String tipoTexto = switch (boleta.getTipoEntrega()) {
                case DOMICILIO -> "Envío a domicilio";
                case RECOGIDA_EN_TIENDA -> "Recoger en tienda";
                case AGENCIA -> "Envío por agencia";
            };
            sb.append("      <div><strong>Tipo de entrega:</strong> <span class=\"badge\">").append(tipoTexto).append("</span></div>");
        }
        sb.append("    </div>");

        // Dirección (solo si existe)
        if (boleta.getDireccionEnvio() != null && !boleta.getDireccionEnvio().trim().isEmpty()) {
            sb.append("    <p><strong>Dirección de envío:</strong><br>")
                    .append(escapeHtml(boleta.getDireccionEnvio().trim()));
            if (boleta.getReferenciaEnvio() != null && !boleta.getReferenciaEnvio().trim().isEmpty()) {
                sb.append(" - ").append(escapeHtml(boleta.getReferenciaEnvio().trim()));
            }
            if (boleta.getDistrito() != null) {
                sb.append("<br>").append(escapeHtml(boleta.getDistrito()));
                if (boleta.getDepartamento() != null) sb.append(" - ").append(escapeHtml(boleta.getDepartamento()));
            }
            sb.append("</p>");
        } else {
            sb.append("    <p><strong>Entrega:</strong> Se coordinará por WhatsApp con el cliente.</p>");
        }

        sb.append("    <h3 style=\"border-bottom:2px solid #e2e8f0; padding-bottom:8px;\">Productos Pedidos</h3>")
                .append(buildProductsTableEnhanced(boleta))
                .append("    <div class=\"total\">")
                .append("      Total: S/ ").append(formatPrice(boleta.getTotal())).append("")
                .append("    </div>")
                .append("  </div>")
                .append("  <div class=\"footer\">")
                .append("    <p>StoreCollection © ").append(LocalDate.now().getYear()).append(" • Mensaje automático</p>")
                .append("  </div>")
                .append("</div>")
                .append("</body></html>");

        return sb.toString();
    }

    private String buildEmailHtmlForCustomer(Boleta boleta) {
        StringBuilder sb = new StringBuilder(4096);

        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"es\">")
                .append("<head>")
                .append("  <meta charset=\"UTF-8\">")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("  <title>¡Gracias por tu compra! #").append(boleta.getId()).append("</title>")
                .append("  <style>")
                .append("    body { font-family: 'Segoe UI', Arial, sans-serif; margin:0; padding:0; background:#f0fdf4; color:#166534; }")
                .append("    .container { max-width:640px; margin:30px auto; background:white; border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,0.1); overflow:hidden; }")
                .append("    .header { background: linear-gradient(135deg, #10b981, #34d399); color:white; padding:40px 32px; text-align:center; }")
                .append("    .header h1 { margin:0; font-size:30px; }")
                .append("    .content { padding:32px; color:#1e293b; }")
                .append("    table { width:100%; border-collapse:collapse; margin:24px 0; font-size:15px; }")
                .append("    th { background:#f0fdf4; text-align:left; padding:14px 12px; color:#166534; font-weight:600; }")
                .append("    td { padding:14px 12px; border-bottom:1px solid #bbf7d0; }")
                .append("    .text-right { text-align:right; }")
                .append("    .text-center { text-align:center; }")
                .append("    .total { background:#ecfdf5; padding:20px; text-align:right; font-size:22px; font-weight:bold; color:#166534; border-top:3px solid #10b981; }")
                .append("    .footer { background:#f0fdf4; padding:24px; text-align:center; font-size:13px; color:#166534; }")
                .append("  </style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("  <div class=\"header\">")
                .append("    <h1>¡Gracias por tu compra!</h1>")
                .append("    <p style=\"margin:10px 0 0; font-size:19px;\">Pedido #").append(boleta.getId()).append("</p>")
                .append("  </div>")
                .append("  <div class=\"content\">")
                .append("    <h2 style=\"color:#166534;\">¡Hola ").append(escapeHtml(boleta.getCompradorNombre())).append("! 👋</h2>")
                .append("    <p>Hemos recibido tu pedido correctamente. ¡Gracias por confiar en nosotros!</p>")
                .append("    <p><strong>Pronto nos pondremos en contacto contigo por WhatsApp</strong> para coordinar la entrega y confirmarte los detalles.</p>")

                .append("    <h3 style=\"border-bottom:2px solid #bbf7d0; padding-bottom:8px; color:#166534;\">Resumen de tu pedido</h3>")
                .append(buildProductsTableEnhanced(boleta))
                .append("    <div class=\"total\">")
                .append("      Total: S/ ").append(formatPrice(boleta.getTotal())).append("")
                .append("    </div>")
                .append("    <p style=\"margin-top:32px; font-size:16px;\">¡Estamos preparando todo con mucho cariño! 🎁</p>")
                .append("    <p>Si tienes alguna duda, responde este correo o escríbenos por WhatsApp.</p>")
                .append("  </div>")
                .append("  <div class=\"footer\">")
                .append("    <p><strong>").append(escapeHtml(boleta.getTienda().getNombre())).append("</strong> © ").append(LocalDate.now().getYear()).append("</p>")
                .append("    <p>Mensaje automático – Puedes responder si necesitas ayuda</p>")
                .append("  </div>")
                .append("</div>")
                .append("</body></html>");

        return sb.toString();
    }

    // Tabla mejorada: muestra variantes si existen
    private String buildProductsTableEnhanced(Boleta boleta) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>")
                .append("<thead><tr>")
                .append("<th>Producto</th>")
                .append("<th class=\"text-center\">Cant.</th>")
                .append("<th class=\"text-right\">Precio</th>")
                .append("<th class=\"text-right\">Subtotal</th>")
                .append("</tr></thead>")
                .append("<tbody>");

        for (BoletaDetalle d : boleta.getDetalles()) {
            ProductoVariante v = d.getVariante();
            String nombre = escapeHtml(v.getProducto().getNombre());

            // Variantes
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

    // Helpers útiles
    private String orDash(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : "—";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", price);
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
            throw new IllegalStateException("El carrito está vacío");
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + request.getTiendaId()));

        String numeroWhats = normalizeWhatsappNumber(tienda.getWhatsapp());
        if (numeroWhats == null || numeroWhats.length() < 9) {
            throw new IllegalStateException("Número de WhatsApp inválido: " + tienda.getWhatsapp());
        }

        StringBuilder msg = new StringBuilder();

        // Encabezado
        msg.append("🛒 *¡NUEVO PEDIDO RECIBIDO!*\n");
        msg.append("*").append(escapeMarkdown(tienda.getNombre().toUpperCase())).append("*\n");
        msg.append("📅 ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        // Detalle de productos
        msg.append("📦 *PRODUCTOS SOLICITADOS*\n\n");

        BigDecimal total = BigDecimal.ZERO;
        int itemNumber = 1;

        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();
            if (variante == null || variante.getProducto() == null) continue;

            Producto producto = variante.getProducto();
            BigDecimal precioUnit = variante.getPrecio();
            BigDecimal subtotal = precioUnit.multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            msg.append(itemNumber++).append(". *").append(escapeMarkdown(producto.getNombre())).append("*\n");
            msg.append("   Cantidad: ").append(item.getCantidad()).append(" und.\n");

            // Variantes
            if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                msg.append("   🎨 Opciones seleccionadas:\n");
                for (AtributoValor av : variante.getAtributos()) {
                    String attrName = av.getAtributo() != null ? av.getAtributo().getNombre() : "Opción";
                    msg.append("      • ").append(escapeMarkdown(attrName)).append(": *").append(escapeMarkdown(av.getValor())).append("*\n");
                }
            }

            // SKU si existe
            if (variante.getSku() != null && !variante.getSku().trim().isEmpty()) {
                msg.append("   🏷️ SKU: ").append(variante.getSku().trim()).append("\n");
            }

            msg.append("   💵 Precio: S/ ").append(formatPrice(precioUnit))
                    .append(" → Subtotal: S/ ").append(formatPrice(subtotal)).append("\n\n");
        }

        // Resumen
        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("📊 *RESUMEN DEL PEDIDO*\n");
        msg.append("💰 *TOTAL: S/ ").append(formatPrice(total)).append("*\n\n");

        // Datos para coordinar (espacios en blanco)
        msg.append("👤 *POR FAVOR, COORDINA CON EL CLIENTE:*\n\n");
        msg.append("• Nombre completo: ______________________________\n");
        msg.append("• Teléfono / WhatsApp: __________________________\n");
        msg.append("• Dirección completa (calle, número, referencia): ______________________________\n");
        msg.append("• Distrito: ___________________   Departamento: ___________________\n");
        msg.append("• DNI (para recojo en agencia o tienda): ________________\n");
        msg.append("• Método de entrega: ☐ Domicilio   ☐ Recojo en tienda   ☐ Agencia\n");
        msg.append("• Forma de pago: ☐ Transferencia   ☐ Yape/Plin   ☐ Contra entrega   ☐ Tarjeta\n\n");

        // Cierre
        msg.append("✅ *Responde este mensaje para confirmar disponibilidad, total final y coordinar la entrega.*\n\n");
        msg.append("¡Gracias por atender rápido! 🙌");

        String encodedMessage = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);

        // Limpiar carrito
        limpiarCarrito(request.getSessionId());

        return "https://wa.me/" + numeroWhats + "?text=" + encodedMessage;
    }
    // Pequeño helper para evitar problemas con _ * en nombres de productos o atributos
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`");
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