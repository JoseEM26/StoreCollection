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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarritoServiceImpl implements CarritoService {

    private final JavaMailSender mailSender;  // ← NUEVO: inyectar esto
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

    @Override
    public BoletaResponse checkoutOnline(BoletaRequest request) {
        List<Carrito> items = repository.findBySessionId(request.getSessionId());
        if (items.isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        BigDecimal total = BigDecimal.ZERO;
        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();
            if (variante == null) {
                throw new RuntimeException("Variante no asociada en un ítem del carrito");
            }
            total = total.add(variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        Boleta boleta = new Boleta();
        boleta.setSessionId(request.getSessionId());
        boleta.setTotal(total);
        boleta.setTienda(tienda);
        boleta.setEstado(Boleta.EstadoBoleta.PENDIENTE);

        if (request.getUserId() != null) {
            Usuario user = usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            boleta.setUser(user);
        }

        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();

            BoletaDetalle detalle = new BoletaDetalle();
            detalle.setBoleta(boleta);
            detalle.setVariante(variante);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(variante.getPrecio());
            detalle.setSubtotal(variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));

            boleta.getDetalles().add(detalle);
        }

        boleta = boletaRepository.save(boleta);
        limpiarCarrito(request.getSessionId());

        // Enviar email al admin
        sendEmailNotification(boleta, tienda.getUser().getEmail());

        return toBoletaResponse(boleta);
    }

    private void sendEmailNotification(Boleta boleta, String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("StoreCollection <joseangelespinozamorales@hotmail.com>");
            helper.setTo(toEmail);
            helper.setSubject("Nuevo Pedido #" + boleta.getId() + " - StoreCollection");

            String htmlContent = buildEmailHtml(boleta);
            helper.setText(htmlContent, true); // true = es HTML

            mailSender.send(message);
            System.out.println("Email enviado correctamente a " + toEmail);

        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildEmailHtml(Boleta boleta) {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"es\">")
                .append("<head>")
                .append("  <meta charset=\"UTF-8\">")
                .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("  <title>Nuevo Pedido #").append(boleta.getId()).append("</title>")
                .append("  <style>")
                .append("    body { font-family: -apple-system, BlinkMacC, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 0; background-color: #f6f9fc; color: #333; }")
                .append("    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }")
                .append("    .header { background: #6366f1; color: white; padding: 24px 32px; text-align: center; }")
                .append("    .header h1 { margin: 0; font-size: 24px; }")
                .append("    .content { padding: 32px; }")
                .append("    .total { font-size: 20px; font-weight: 600; margin: 24px 0; text-align: right; color: #1f2937; }")
                .append("    table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
                .append("    th, td { padding: 12px 10px; text-align: left; border-bottom: 1px solid #e5e7eb; }")
                .append("    th { background: #f3f4f6; font-weight: 600; color: #374151; }")
                .append("    .footer { background: #f9fafb; padding: 24px; text-align: center; font-size: 13px; color: #6b7280; border-top: 1px solid #e5e7eb; }")
                .append("    .highlight { color: #6366f1; font-weight: 600; }")
                .append("  </style>")
                .append("</head>")
                .append("<body>")
                .append("  <div class=\"container\">")
                .append("    <div class=\"header\">")
                .append("      <h1>Nuevo Pedido <span class=\"highlight\">#").append(boleta.getId()).append("</span></h1>")
                .append("    </div>")
                .append("    <div class=\"content\">");

        // Información básica
        sb.append("      <p>Hola, se ha recibido un nuevo pedido en <strong>").append(escapeHtml(boleta.getTienda().getNombre())).append("</strong></p>")
                .append("      <p><strong>Fecha:</strong> ").append(boleta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>")
                .append("      <p><strong>Estado:</strong> ").append(boleta.getEstado().name()).append("</p>");

        // Tabla de productos
        sb.append("      <table>")
                .append("        <thead>")
                .append("          <tr>")
                .append("            <th>Producto</th>")
                .append("            <th style=\"text-align:center;\">Cant.</th>")
                .append("            <th style=\"text-align:right;\">Precio unit.</th>")
                .append("            <th style=\"text-align:right;\">Subtotal</th>")
                .append("          </tr>")
                .append("        </thead>")
                .append("        <tbody>");

        for (BoletaDetalle detalle : boleta.getDetalles()) {
            Producto producto = detalle.getVariante().getProducto();
            String nombreProducto = escapeHtml(producto.getNombre());

            sb.append("          <tr>")
                    .append("            <td>").append(nombreProducto).append("</td>")
                    .append("            <td style=\"text-align:center;\">").append(detalle.getCantidad()).append("</td>")
                    .append("            <td style=\"text-align:right;\">S/ ").append(detalle.getPrecioUnitario()).append("</td>")
                    .append("            <td style=\"text-align:right;\">S/ ").append(detalle.getSubtotal()).append("</td>")
                    .append("          </tr>");
        }

        sb.append("        </tbody>")
                .append("      </table>");

        // Total
        sb.append("      <div class=\"total\">")
                .append("        Total: <span style=\"font-size:24px;color:#111827;\">S/ ").append(boleta.getTotal()).append("</span>")
                .append("      </div>");

        sb.append("    </div>")
                .append("    <div class=\"footer\">")
                .append("      <p>StoreCollection © ").append(LocalDate.now().getYear()).append(" | Plataforma de gestión de tiendas</p>")
                .append("      <p>Este es un mensaje automático, por favor no responder directamente.</p>")
                .append("    </div>")
                .append("  </div>")
                .append("</body>")
                .append("</html>");

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

    @Override
    @Transactional(readOnly = true)  // ← OBLIGATORIO para fetch eager y evitar LazyInitializationException
    public String checkoutWhatsapp(BoletaRequest request) {
        // 1. Carga con JOIN FETCH (carga variante, producto, atributos y tienda)
        List<Carrito> items = repository.findBySessionIdWithDetails(request.getSessionId());

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío o la sesión no existe");
        }

        System.out.println("Checkout WhatsApp - Sesión: " + request.getSessionId() +
                " | Items encontrados: " + items.size());

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + request.getTiendaId()));

        String numeroWhats = normalizeWhatsappNumber(tienda.getWhatsapp());
        if (numeroWhats == null || numeroWhats.length() < 9) {
            throw new IllegalStateException("Número de WhatsApp inválido: " + tienda.getWhatsapp());
        }

        // 2. Construcción del mensaje
        StringBuilder msg = new StringBuilder();
        msg.append("🛒 *¡NUEVO PEDIDO EN ").append(tienda.getNombre().toUpperCase()).append("!*\n\n");

        BigDecimal total = BigDecimal.ZERO;
        int itemNumber = 1;
        int validItems = 0;

        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();

            // Log clave para depurar
            System.out.println("Item #" + item.getId() + " | Variante ID: " +
                    (variante != null ? variante.getId() : "NULL") +
                    " | Producto: " + (variante != null && variante.getProducto() != null ?
                    variante.getProducto().getNombre() : "NULL"));

            if (variante == null) {
                msg.append("⚠️ Ítem ").append(itemNumber).append(" - Variante no encontrada (ID: ")
                        .append(item.getVariante().getId()).append(")\n");
                itemNumber++;
                continue;
            }

            Producto producto = variante.getProducto();
            if (producto == null) {
                msg.append("⚠️ Ítem ").append(itemNumber).append(" - Producto no encontrado (Variante: ")
                        .append(variante.getId()).append(")\n");
                itemNumber++;
                continue;
            }

            validItems++;

            // Producto principal
            msg.append("*").append(itemNumber).append(".* ")
                    .append(producto.getNombre())
                    .append(" × ").append(item.getCantidad())
                    .append(" und.\n");

            // Precios
            BigDecimal precioUnit = variante.getPrecio();
            BigDecimal subtotal = precioUnit.multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            msg.append("   💵 Precio: S/ ").append(formatPrice(precioUnit))
                    .append("   → Subtotal: S/ ").append(formatPrice(subtotal))
                    .append("\n");

            // SKU
            if (variante.getSku() != null && !variante.getSku().trim().isEmpty()) {
                msg.append("   🏷️ SKU: ").append(variante.getSku()).append("\n");
            }

            // Atributos
            if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                msg.append("   🎨 *Variantes:*\n");
                for (AtributoValor av : variante.getAtributos()) {
                    String nombreAttr = (av.getAtributo() != null && av.getAtributo().getNombre() != null)
                            ? av.getAtributo().getNombre() : "Atributo";
                    msg.append("      • ").append(nombreAttr).append(": *").append(av.getValor()).append("*\n");
                }
            }

            // Imagen
            if (variante.getImagenUrl() != null && !variante.getImagenUrl().trim().isEmpty()) {
                msg.append("   📸 ").append(shortenUrl(variante.getImagenUrl())).append("\n");
            }

            msg.append("\n");
            itemNumber++;
        }

        if (validItems == 0) {
            throw new IllegalStateException("No hay ítems válidos en el carrito (todas las variantes/productos son null)");
        }

        // Resumen
        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("📊 *RESUMEN DEL PEDIDO*\n");
        msg.append("Items válidos: ").append(validItems).append("\n");
        msg.append("💰 *TOTAL A COBRAR: S/ ").append(formatPrice(total)).append("*\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"));
        msg.append("📅 Generado: ").append(LocalDateTime.now().format(dtf)).append("\n\n");

        msg.append("👤 *DATOS DEL CLIENTE (coordinar):*\n");
        msg.append("• Nombre: ______________________\n");
        msg.append("• Teléfono: ____________________\n");
        msg.append("• Dirección: ___________________\n");
        msg.append("• Pago: ________________________\n\n");

        msg.append("✅ *¡Listo para procesar!* Responde para confirmar 🙌");

        // Encoding seguro
        String encodedMessage;
        try {
            encodedMessage = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Error encoding mensaje WhatsApp: " + e.getMessage());
            encodedMessage = URLEncoder.encode(msg.toString().replaceAll("[^\\p{ASCII}]", "?"), StandardCharsets.UTF_8);
        }

        // Limpieza
        limpiarCarrito(request.getSessionId());

        // URL final
        String url = "https://wa.me/" + numeroWhats + "?text=" + encodedMessage;
        System.out.println("URL WhatsApp generada exitosamente: " + url);

        return url;
    }
    // MÉTODOS AUXILIARES (agregar estos private methods)
    private String normalizeWhatsappNumber(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("00")) cleaned = cleaned.substring(2);
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);
        if (cleaned.length() == 9 && !cleaned.startsWith("51")) cleaned = "51" + cleaned; // Perú
        return (cleaned.length() >= 10 && cleaned.length() <= 14) ? cleaned : null;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", price);
    }

    private String shortenUrl(String url) {
        if (url.length() > 50) {
            return url.substring(0, 47) + "...";
        }
        return url;
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
                        attr.setNombre(av.getAtributo().getNombre());
                        attr.setValor(av.getValor());
                        attr.setTiendaId(av.getAtributo().getTienda().getId());
                        return attr;
                    })
                    .collect(Collectors.toList());

            dto.setAtributos(atributos.isEmpty() ? null : atributos);
        }
        return dto;
    }

    // ===================== CHECKOUT =====================



    private BoletaResponse toBoletaResponse(Boleta boleta) {
        BoletaResponse response = new BoletaResponse();
        response.setId(boleta.getId());
        response.setSessionId(boleta.getSessionId());
        response.setTiendaId(boleta.getTienda().getId());
        response.setTotal(boleta.getTotal());
        response.setFecha(boleta.getFecha().toString());
        response.setEstado(boleta.getEstado().name());

        if (boleta.getUser() != null) {
            response.setUserId(boleta.getUser().getId());
        }

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
                    attr.setNombre(av.getAtributo().getNombre());
                    attr.setValor(av.getValor());
                    attr.setTiendaId(av.getAtributo().getTienda().getId());
                    return attr;
                })
                .collect(Collectors.toList());

        dto.setAtributos(atributos.isEmpty() ? null : atributos);

        return dto;
    }
}