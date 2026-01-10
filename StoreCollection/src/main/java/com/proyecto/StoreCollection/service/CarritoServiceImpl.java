package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository repository;
    private final ProductoVarianteRepository varianteRepository;
    private final TiendaRepository tiendaRepository;

    // ===================== CARRITO =====================

    @Override
    @Transactional(readOnly = true)
    public List<CarritoResponse> findBySessionId(String sessionId, Integer tiendaId) {
        if (tiendaId == null) {
            throw new IllegalArgumentException("tiendaId es requerido");
        }
        return repository.findBySessionIdAndTiendaId(sessionId, tiendaId)
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
    @Transactional
    public CarritoResponse crear(CarritoRequest request) {
        ProductoVariante variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + request.getVarianteId()));

        Tienda tienda = variante.getProducto().getTienda();

        Carrito carrito = new Carrito();
        carrito.setSessionId(request.getSessionId());
        carrito.setTienda(tienda);
        carrito.setVariante(variante);
        carrito.setCantidad(request.getCantidad());

        Carrito saved = repository.save(carrito);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CarritoResponse actualizar(Integer carritoId, CarritoRequest request) {
        // Validación estricta del ID
        if (carritoId == null || carritoId <= 0) {
            throw new IllegalArgumentException("El ID del carrito es requerido y debe ser un número positivo válido para actualizar cantidad");
        }

        Carrito carrito = repository.findById(carritoId)
                .orElseThrow(() -> new IllegalArgumentException("Carrito no encontrado con ID: " + carritoId));

        ProductoVariante variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + request.getVarianteId()));

        // Seguridad: no permitir cambiar variante o tienda
        if (!carrito.getVariante().getId().equals(variante.getId())) {
            throw new IllegalStateException("No se puede cambiar la variante de un item existente");
        }
        if (!carrito.getTienda().getId().equals(variante.getProducto().getTienda().getId())) {
            throw new IllegalStateException("No se puede cambiar la tienda de un item existente");
        }

        carrito.setCantidad(request.getCantidad());

        Carrito saved = repository.save(carrito);
        return toResponse(saved);
    }
    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public void limpiarCarrito(String sessionId, Integer tiendaId) {
        if (tiendaId == null) {
            throw new IllegalArgumentException("tiendaId es requerido para limpiar carrito");
        }
        repository.deleteBySessionIdAndTiendaId(sessionId, tiendaId);
    }

    // ===================== CHECKOUT ONLINE =====================









    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return String.format(Locale.forLanguageTag("es-PE"), "%.2f", price);
    }

    @Override
    @Transactional(readOnly = true)
    public String checkoutWhatsapp(BoletaRequest req) {

        List<Carrito> items = repository.findBySessionIdAndTiendaIdWithDetails(
                req.getSessionId(), req.getTiendaId());

        if (items.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío para esta tienda");
        }

        Tienda tienda = tiendaRepository.findById(req.getTiendaId())
                .orElseThrow(() -> new IllegalArgumentException("Tienda no encontrada: " + req.getTiendaId()));

        String numeroWhats = normalizeWhatsappNumber(tienda.getWhatsapp());
        if (numeroWhats == null || numeroWhats.length() < 9) {
            throw new IllegalStateException("Número de WhatsApp de la tienda inválido");
        }

        StringBuilder msg = new StringBuilder();

        msg.append("🛒 *¡NUEVO PEDIDO!*\n")
                .append("*").append(escapeMarkdown(tienda.getNombre().toUpperCase())).append("*\n")
                .append("📅 ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")

                .append("📦 *PRODUCTOS*\n\n");

        BigDecimal total = BigDecimal.ZERO;
        int itemNumber = 1;

        for (Carrito item : items) {
            ProductoVariante v = item.getVariante();
            if (v == null || v.getProducto() == null) continue;

            BigDecimal precio = v.getPrecio();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            msg.append(itemNumber++).append(". *").append(escapeMarkdown(v.getProducto().getNombre())).append("*\n");

            // Variantes (si existen)
            if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
                msg.append("   Opciones:\n");
                for (AtributoValor av : v.getAtributos()) {
                    msg.append("      • ").append(escapeMarkdown(av.getAtributo().getNombre()))
                            .append(": ").append(escapeMarkdown(av.getValor())).append("\n");
                }
            }

            msg.append("   Cantidad: ").append(item.getCantidad()).append(" und.\n")
                    .append("   Precio: S/ ").append(formatPrice(precio))
                    .append(" → Subtotal: S/ ").append(formatPrice(subtotal)).append("\n\n");
        }

        msg.append("━━━━━━━━━━━━━━━━━━━\n")
                .append("💰 *TOTAL: S/ ").append(formatPrice(total)).append("*\n\n")

                // Mensaje más directo y limpio (sin pedir datos personales de entrada)
                .append("Por favor responde con:\n")
                .append("• Dirección completa + referencia\n")
                .append("• Distrito\n")
                .append("• Método de pago\n")
                .append("• Método de entrega\n\n")

                .append("¡Gracias por tu pedido! Te respondo rapidito 🚀");

        String encoded = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8);
        return "https://wa.me/" + numeroWhats + "?text=" + encoded;
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
        response.setCompradorNumero(boleta.getCompradorNumero());

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