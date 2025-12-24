// Nuevo: BoletaServiceImpl (para admin, con deducción de stock al atender)
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.response.*;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BoletaServiceImpl implements BoletaService {

    private final BoletaRepository repository;
    private final ProductoVarianteRepository varianteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BoletaResponse> findByTienda(Integer tiendaId) {
        return repository.findByTiendaId(tiendaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoletaResponse> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaResponse findById(Integer id) {
        Boleta boleta = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + id));
        return toResponse(boleta);
    }

    @Override
    public BoletaResponse updateEstado(Integer id, String estadoStr) {
        Boleta boleta = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada: " + id));

        Boleta.EstadoBoleta nuevoEstado = Boleta.EstadoBoleta.valueOf(estadoStr.toUpperCase());

        if (nuevoEstado == Boleta.EstadoBoleta.ATENDIDA && boleta.getEstado() == Boleta.EstadoBoleta.PENDIENTE) {
            for (BoletaDetalle detalle : boleta.getDetalles()) {
                ProductoVariante variante = detalle.getVariante();
                if (variante.getStock() < detalle.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para " + variante.getProducto().getNombre());
                }
                variante.setStock(variante.getStock() - detalle.getCantidad());
                varianteRepository.save(variante);
            }
        }

        boleta.setEstado(nuevoEstado);
        return toResponse(repository.save(boleta));
    }

    private BoletaResponse toResponse(Boleta boleta) {
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
                        .map(this::toDetalleResponse)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private BoletaDetalleResponse toDetalleResponse(BoletaDetalle detalle) {
        // Similar a toBoletaDetalleResponse en CarritoServiceImpl
        BoletaDetalleResponse dto = new BoletaDetalleResponse();
        // ... (copia el código de toBoletaDetalleResponse)
        return dto;
    }
}