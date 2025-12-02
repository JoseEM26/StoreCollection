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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
// ProductoServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug) {
        List<Object[]> rows = productoRepository.findRawCatalogByTiendaSlug(tiendaSlug);

        Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer id = (Integer) row[0];
            String nombre = (String) row[1];
            String slug = (String) row[2];
            String nombreCategoria = (String) row[3];

            ProductoCardResponse dto = map.computeIfAbsent(id, key -> {
                ProductoCardResponse p = new ProductoCardResponse();
                p.setId(id);
                p.setNombre(nombre);
                p.setSlug(slug);
                p.setNombreCategoria(nombreCategoria);
                p.setVariantes(new ArrayList<>());
                return p;
            });

            // Añadir variante si existe
            BigDecimal precio = row[4] != null ? (BigDecimal) row[4] : null;
            Integer stock = row[5] != null ? (Integer) row[5] : null;
            String imagenUrl = (String) row[6];
            Boolean activo = row[7] != null ? (Boolean) row[7] : false;

            if (activo != null && activo && precio != null) {
                ProductoCardResponse.VarianteCard v = new ProductoCardResponse.VarianteCard();
                v.setPrecio(precio);
                v.setStock(stock != null ? stock : 0);
                v.setImagenUrl(imagenUrl);
                v.setActivo(true);
                dto.getVariantes().add(v);
            }
        }

        // Filtrar solo productos con stock y ordenar por más vendidos
        return map.values().stream()
                .filter(p -> p.getStockTotal() > 0)
                .sorted((a, b) -> Integer.compare(b.getStockTotal(), a.getStockTotal()))
                .toList();
    }

    // ProductoServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public ProductoCardResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug) {
        List<Object[]> rows = productoRepository.findRawDetailBySlugs(tiendaSlug, productoSlug);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }

        Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer id = (Integer) row[0];
            String nombre = (String) row[1];
            String slug = (String) row[2];
            String nombreCategoria = (String) row[3];

            ProductoCardResponse dto = map.computeIfAbsent(id, k -> {
                ProductoCardResponse p = new ProductoCardResponse();
                p.setId(id);
                p.setNombre(nombre);
                p.setSlug(slug);
                p.setNombreCategoria(nombreCategoria);
                p.setVariantes(new ArrayList<>());
                return p;
            });

            BigDecimal precio = row[4] != null ? (BigDecimal) row[4] : null;
            Integer stock = row[5] != null ? (Integer) row[5] : 0;
            String imagenUrl = (String) row[6];
            Boolean activoVar = row[7] != null ? (Boolean) row[7] : false;

            if (activoVar && precio != null) {
                var v = new ProductoCardResponse.VarianteCard();
                v.setPrecio(precio);
                v.setStock(stock);
                v.setImagenUrl(imagenUrl);
                v.setActivo(true);
                dto.getVariantes().add(v);
            }
        }

        // Ahora sí es seguro: siempre hay al menos un producto
        return map.values().iterator().next();
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