package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.BoletaRequest;
import com.proyecto.StoreCollection.dto.request.CarritoRequest;
import com.proyecto.StoreCollection.dto.response.BoletaDetalleResponse;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import com.proyecto.StoreCollection.dto.response.CarritoResponse;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CarritoServiceImpl implements CarritoService {

    @Autowired
    private CarritoRepository repository;

    @Autowired
    private ProductoVarianteRepository varianteRepository;

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
        Carrito carrito = id == null ? new Carrito() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + id));

        carrito.setSessionId(request.getSessionId());
        carrito.setCantidad(request.getCantidad());

        if (request.getVarianteId() != null) {
            ProductoVariante v = varianteRepository.findById(request.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada"));
            carrito.setVariante(v);
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

    /** Mapeo completo de Carrito → CarritoResponse con datos ricos */
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

            // Combinar atributos (Color, Talla, etc.)
            String atributos = v.getAtributos().stream()
                    .map(av -> av.getValor())
                    .collect(Collectors.joining(", "));
            dto.setAtributos(atributos.isEmpty() ? null : atributos);
        }
        return dto;
    }

    // ===================== CHECKOUT =====================

    @Override
    public BoletaResponse checkout(BoletaRequest request) {
        List<Carrito> items = repository.findBySessionId(request.getSessionId());
        if (items.isEmpty()) {
            throw new RuntimeException("Carrito vacío");
        }

        // Validar stock y calcular total
        BigDecimal total = BigDecimal.ZERO;
        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();
            if (variante.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para SKU: " + variante.getSku());
            }
            total = total.add(variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        // Crear boleta
        Boleta boleta = new Boleta();
        boleta.setSessionId(request.getSessionId());
        boleta.setTotal(total);

        if (request.getUserId() != null) {
            Usuario user = usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            boleta.setUser(user);
        }

        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        boleta.setTienda(tienda);

        // Crear detalles y reducir stock
        for (Carrito item : items) {
            ProductoVariante variante = item.getVariante();

            BoletaDetalle detalle = new BoletaDetalle();
            detalle.setBoleta(boleta);
            detalle.setVariante(variante);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(variante.getPrecio());
            detalle.setSubtotal(variante.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));

            boleta.getDetalles().add(detalle);

            // Reducir stock
            variante.setStock(variante.getStock() - item.getCantidad());
            varianteRepository.save(variante);
        }

        boleta.setEstado(Boleta.EstadoBoleta.COMPLETADA);
        boleta = boletaRepository.save(boleta);

        // Limpiar carrito
        limpiarCarrito(request.getSessionId());

        return toBoletaResponse(boleta);
    }

    private BoletaResponse toBoletaResponse(Boleta boleta) {
        BoletaResponse response = new BoletaResponse();

        response.setId(boleta.getId());
        response.setSessionId(boleta.getSessionId());

        if (boleta.getUser() != null) {
            response.setUserId(boleta.getUser().getId());
        }

        response.setTiendaId(boleta.getTienda().getId());
        response.setTotal(boleta.getTotal());
        response.setFecha(boleta.getFecha());
        response.setEstado(boleta.getEstado().name());

        response.setDetalles(
                boleta.getDetalles() != null
                        ? boleta.getDetalles().stream()
                        .map(this::toBoletaDetalleResponse)
                        .collect(Collectors.toList())
                        : new ArrayList<>()
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

        return dto;
    }
}