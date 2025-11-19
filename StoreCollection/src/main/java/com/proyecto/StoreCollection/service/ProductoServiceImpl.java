// src/main/java/com/proyecto/StoreCollection/service/ProductoServiceImpl.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@Transactional
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiendaService tiendaService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> findAll(Pageable pageable) {
        return productoRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findMisProductos() {
        return productoRepository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findByTiendaSlug(String tiendaSlug) {
        return productoRepository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug) {
        Producto p = productoRepository.findByTiendaSlugAndProductoSlug(tiendaSlug, productoSlug)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findByCategoriaId(Long categoriaId) {
        return productoRepository.findByCategoriaIdSafe(categoriaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse findById(Long id) {
        return toResponse(productoRepository.getByIdAndTenant(id));
    }

    @Override
    public ProductoResponse save(ProductoRequest request) {
        return save(request, null);
    }

    @Override
    public ProductoResponse save(ProductoRequest request, Long id) {
        Producto p = id == null ? new Producto() : productoRepository.getByIdAndTenant(id);

        p.setNombre(request.getNombre());
        p.setSlug(request.getSlug());

        Categoria c = categoriaRepository.getByIdAndTenant(request.getCategoriaId());
        p.setCategoria(c);
        p.setTienda(tiendaService.getTiendaDelUsuarioActual());

        return toResponse(productoRepository.save(p));
    }

    @Override
    public void deleteById(Long id) {
        productoRepository.delete(productoRepository.getByIdAndTenant(id));
    }

    // AHORA SÍ FUNCIONA
    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getSlug(),
                p.getCategoria().getId(),
                p.getCategoria().getNombre(),
                p.getTienda().getId()
        );
    }
}