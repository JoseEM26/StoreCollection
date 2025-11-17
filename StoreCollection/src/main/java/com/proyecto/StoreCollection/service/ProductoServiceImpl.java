package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> findAll(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findByTiendaId(Long tiendaId) {
        return productoRepository.findByTiendaId(tiendaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findByCategoriaId(Long categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse findById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        return toResponse(producto);
    }

    @Override
    public ProductoResponse save(ProductoRequest request) { return save(request, null); }

    @Override
    public ProductoResponse save(ProductoRequest request, Long id) {
        Producto p = id == null ? new Producto() : productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        p.setNombre(request.getNombre());
        p.setSlug(request.getSlug());
        Categoria c = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        Tienda t = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        p.setCategoria(c);
        p.setTienda(t);

        return toResponse(productoRepository.save(p));
    }
    @Override
    public void deleteById(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no encontrado: " + id);
        }
        productoRepository.deleteById(id);
    }

    private ProductoResponse toResponse(Producto p) {
        ProductoResponse dto = new ProductoResponse();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setSlug(p.getSlug());
        dto.setCategoriaId(p.getCategoria().getId());
        dto.setTiendaId(p.getTienda().getId());
        return dto;
    }
}