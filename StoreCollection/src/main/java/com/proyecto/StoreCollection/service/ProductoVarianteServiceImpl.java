// src/main/java/com/proyecto/StoreCollection/service/ProductoVarianteServiceImpl.java

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductoVarianteServiceImpl implements ProductoVarianteService {

    private final ProductoVarianteRepository varianteRepository;
    private final ProductoRepository productoRepository;
    private final AtributoValorRepository atributoValorRepository;
    private final TiendaService tiendaService; // ← para seguridad

    // PÚBLICO: catálogo
    @Override
    @Transactional(readOnly = true)
    public List<ProductoVarianteResponse> findByTiendaSlugAndProductoSlug(
            String tiendaSlug, String productoSlug) {

        return varianteRepository.findByTiendaSlugAndProductoSlug(tiendaSlug, productoSlug)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // PRIVADO: panel del dueño
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoVarianteResponse> findAll(Pageable pageable) {
        return varianteRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoVarianteResponse> findByProductoId(Integer productoId) {
        // SEGURIDAD: el producto debe ser del dueño
        productoRepository.getByIdAndTenant(productoId);
        return varianteRepository.findByProductoIdSafe(productoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoVarianteResponse findById(Integer id) {
        return toResponse(varianteRepository.getByIdAndTenant(id));
    }

    @Override
    public ProductoVarianteResponse save(ProductoVarianteRequest request) {
        return save(request, null);
    }

    @Override
    public ProductoVarianteResponse save(ProductoVarianteRequest request, Integer id) {
        ProductoVariante v = id == null
                ? new ProductoVariante()
                : varianteRepository.getByIdAndTenant(id); // ← solo puede editar las suyas

        v.setSku(request.getSku());
        v.setPrecio(request.getPrecio());
        v.setStock(request.getStock());
        v.setImagenUrl(request.getImagenUrl());
        v.setActivo(request.getActivo());

        // SEGURIDAD: solo puede usar productos de su tienda
        Producto p = productoRepository.getByIdAndTenant(request.getProductoId());
        v.setProducto(p);

        // Atributos (colores, tallas, etc.)
        if (request.getAtributoValorIds() != null && !request.getAtributoValorIds().isEmpty()) {
            Set<AtributoValor> attrs = atributoValorRepository.findAllById(request.getAtributoValorIds())
                    .stream()
                    .collect(Collectors.toSet());
            v.setAtributos(attrs);
        } else {
            v.setAtributos(Set.of());
        }

        return toResponse(varianteRepository.save(v));
    }

    @Override
    public void deleteById(Integer id) {
        varianteRepository.delete(varianteRepository.getByIdAndTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoVarianteResponse> findByProductoSlug(String tiendaSlug, String productoSlug) {
        return varianteRepository.findByTiendaSlugAndProductoSlug(tiendaSlug, productoSlug)
                .stream()
                .map(this::toResponse)
                .toList();
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
                    avDto.setAtributoNombre(av.getAtributo().getNombre());
                    return avDto;
                })
                .collect(Collectors.toSet());
        dto.setAtributos(attrs);
        return dto;
    }
}