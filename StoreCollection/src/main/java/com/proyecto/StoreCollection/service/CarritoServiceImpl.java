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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarritoServiceImpl implements CarritoService {

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

    @Override
    public BoletaResponse checkout(BoletaRequest request) {
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
            if (variante.getStock() < item.getCantidad()) {
                throw new RuntimeException(
                        String.format("Stock insuficiente para %s (%s): solo hay %d disponibles",
                                variante.getProducto().getNombre(), variante.getSku(), variante.getStock())
                );
            }
            total = total.add(variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        Boleta boleta = new Boleta();
        boleta.setSessionId(request.getSessionId());
        boleta.setTotal(total);
        boleta.setTienda(tienda);
        boleta.setEstado(Boleta.EstadoBoleta.COMPLETADA);

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

            variante.setStock(variante.getStock() - item.getCantidad());
            varianteRepository.save(variante);
        }

        boleta = boletaRepository.save(boleta);
        limpiarCarrito(request.getSessionId());

        return toBoletaResponse(boleta);
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