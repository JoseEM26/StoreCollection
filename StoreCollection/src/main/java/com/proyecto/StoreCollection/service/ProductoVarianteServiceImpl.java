package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoVarianteRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.dto.response.ProductoVarianteResponse;
import com.proyecto.StoreCollection.entity.AtributoValor;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.entity.ProductoVariante;
import com.proyecto.StoreCollection.repository.AtributoValorRepository;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import com.proyecto.StoreCollection.repository.ProductoVarianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoVarianteServiceImpl implements ProductoVarianteService {

    @Autowired
    private ProductoVarianteRepository repository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AtributoValorRepository atributoValorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoVarianteResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoVarianteResponse> findByProductoId(Long productoId) {
        return repository.findByProductoId(productoId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoVarianteResponse findById(Long id) {
        ProductoVariante variante = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + id));
        return toResponse(variante);
    }

    @Override
    public ProductoVarianteResponse save(ProductoVarianteRequest request) { return save(request, null); }

    @Override
    public ProductoVarianteResponse save(ProductoVarianteRequest request, Long id) {
        ProductoVariante v = id == null ? new ProductoVariante() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + id));

        v.setSku(request.getSku());
        v.setPrecio(request.getPrecio());
        v.setStock(request.getStock());
        v.setImagenUrl(request.getImagenUrl());
        v.setActivo(request.getActivo());

        Producto p = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        v.setProducto(p);

        if (request.getAtributoValorIds() != null) {
            v.setAtributos(atributoValorRepository.findAllById(request.getAtributoValorIds()).stream().collect(Collectors.toSet()));
        }

        return toResponse(repository.save(v));
    }

    @Override
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Variante no encontrada: " + id);
        }
        repository.deleteById(id);
    }

    private ProductoVarianteResponse toResponse(ProductoVariante v) {
        ProductoVarianteResponse dto = new ProductoVarianteResponse();
        dto.setId(v.getId());
        dto.setSku(v.getSku());
        dto.setPrecio(v.getPrecio());
        dto.setStock(v.getStock());
        dto.setImagenUrl(v.getImagenUrl());
        dto.setActivo(v.getActivo());
        dto.setProductoId(v.getProducto().getId());

        Set<AtributoValorResponse> attrs = v.getAtributos().stream()
                .map(av -> {
                    AtributoValorResponse avDto = new AtributoValorResponse();
                    avDto.setId(av.getId());
                    avDto.setValor(av.getValor());
                    avDto.setAtributoId(av.getAtributo().getId());
                    return avDto;
                })
                .collect(Collectors.toSet());
        dto.setAtributos(attrs);
        return dto;
    }
}