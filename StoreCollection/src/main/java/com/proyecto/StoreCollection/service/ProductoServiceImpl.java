// src/main/java/com/proyecto/StoreCollection/service/ProductoServiceImpl.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
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
// En tu ProductoService.java (el que ya tienes)

    public List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug) {
        List<Producto> productos = productoRepository.findByTiendaSlugPublic(tiendaSlug);

        return productos.stream().map(p -> {
            ProductoCardResponse dto = new ProductoCardResponse();
            dto.setId(p.getId());
            dto.setNombre(p.getNombre());
            dto.setSlug(p.getSlug());
            dto.setNombreCategoria(p.getCategoria().getNombre());

            List<ProductoCardResponse.VarianteCard> vars = p.getVariantes().stream()
                    .filter(v -> v.getActivo() != null && v.getActivo())
                    .map(v -> {
                        ProductoCardResponse.VarianteCard vc = new ProductoCardResponse.VarianteCard();
                        vc.setPrecio(v.getPrecio());
                        vc.setStock(v.getStock());
                        vc.setImagenUrl(v.getImagenUrl());
                        vc.setActivo(true);
                        return vc;
                    })
                    .toList();

            dto.setVariantes(vars);
            return dto;
        }).toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> findByCategoriaId(Integer categoriaId) {
        return productoRepository.findByCategoriaIdSafe(categoriaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse findById(Integer id) {
        return toResponse(productoRepository.getByIdAndTenant(id));
    }

    @Override
    public ProductoResponse save(ProductoRequest request) {
        return save(request, null);
    }

    @Override
    public ProductoResponse save(ProductoRequest request, Integer id) {
        Producto p = id == null ? new Producto() : productoRepository.getByIdAndTenant(id);

        p.setNombre(request.getNombre());
        p.setSlug(request.getSlug());

        Categoria c = categoriaRepository.getByIdAndTenant(request.getCategoriaId());
        p.setCategoria(c);
        p.setTienda(tiendaService.getTiendaDelUsuarioActual());

        return toResponse(productoRepository.save(p));
    }

    @Override
    public void deleteById(Integer id) {
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