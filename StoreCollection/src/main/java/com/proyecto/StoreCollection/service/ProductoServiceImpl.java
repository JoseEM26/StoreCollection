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
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiendaService tiendaService;

    // ===================================================================
    // MÉTODOS PRIVADOS (ADMIN / OWNER)
    // ===================================================================

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

    // ===================================================================
    // MÉTODOS PÚBLICOS (CATÁLOGO Y DETALLE) ← LOS QUE IMPORTAN AHORA
    // ===================================================================

    /**
     * Catálogo público: lista todos los productos de una tienda con precio mínimo, stock e imagen
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug) {
        List<Object[]> rows = productoRepository.findRawCatalogByTiendaSlug(tiendaSlug);

        Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer id = (Integer) row[0];

            ProductoCardResponse p = map.computeIfAbsent(id, k -> {
                ProductoCardResponse dto = new ProductoCardResponse();
                dto.setId(id);
                dto.setNombre((String) row[1]);
                dto.setSlug((String) row[2]);
                dto.setNombreCategoria((String) row[3]);
                dto.setVariantes(new ArrayList<>()); // importante inicializar
                return dto;
            });

            BigDecimal precio = row[4] != null ? (BigDecimal) row[4] : null;
            Integer stock = row[5] != null ? (Integer) row[5] : 0;
            String imagenUrl = (String) row[6];
            Boolean activoVar = row[7] != null ? (Boolean) row[7] : false;

            if (activoVar && precio != null) {
                ProductoCardResponse.VarianteCard v = new ProductoCardResponse.VarianteCard();
                v.setPrecio(precio);
                v.setStock(stock);
                v.setImagenUrl(imagenUrl);
                v.setActivo(true);
                p.getVariantes().add(v);
            }
        }

        return map.values().stream()
                .peek(this::calcularCamposDerivados)
                .filter(p -> p.getStockTotal() > 0) // solo productos con stock
                .sorted(Comparator.comparingInt(ProductoCardResponse::getStockTotal).reversed())
                .toList();
    }

    /**
     * Detalle público de un producto por slug
     */
    @Override
    @Transactional(readOnly = true)
    public ProductoCardResponse findByTiendaSlugAndProductoSlug(String tiendaSlug, String productoSlug) {
        List<Object[]> rows = productoRepository.findRawDetailBySlugs(tiendaSlug, productoSlug);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado o tienda inactiva");
        }

        Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer id = (Integer) row[0];

            ProductoCardResponse p = map.computeIfAbsent(id, k -> {
                ProductoCardResponse dto = new ProductoCardResponse();
                dto.setId(id);
                dto.setNombre((String) row[1]);
                dto.setSlug((String) row[2]);
                dto.setNombreCategoria((String) row[3]);
                dto.setVariantes(new ArrayList<>());
                return dto;
            });

            BigDecimal precio = row[4] != null ? (BigDecimal) row[4] : null;
            Integer stock = row[5] != null ? (Integer) row[5] : 0;
            String imagenUrl = (String) row[6];
            Boolean activoVar = row[7] != null ? (Boolean) row[7] : false;

            if (activoVar && precio != null) {
                ProductoCardResponse.VarianteCard v = new ProductoCardResponse.VarianteCard();
                v.setPrecio(precio);
                v.setStock(stock);
                v.setImagenUrl(imagenUrl);
                v.setActivo(true);
                p.getVariantes().add(v);
            }
        }

        ProductoCardResponse resultado = map.values().iterator().next();
        calcularCamposDerivados(resultado);
        return resultado;
    }

    /**
     * Método reutilizable: calcula precio mínimo, stock total e imagen principal
     */
    private void calcularCamposDerivados(ProductoCardResponse p) {
        List<ProductoCardResponse.VarianteCard> activas = p.getVariantes().stream()
                .filter(ProductoCardResponse.VarianteCard::isActivo)
                .toList();

        // Stock total
        int stockTotal = activas.stream()
                .mapToInt(ProductoCardResponse.VarianteCard::getStock)
                .sum();
        p.setStockTotal(stockTotal);

        // Precio mínimo
        activas.stream()
                .map(ProductoCardResponse.VarianteCard::getPrecio)
                .min(BigDecimal::compareTo)
                .ifPresentOrElse(
                        p::setPrecioMinimo,
                        () -> p.setPrecioMinimo(BigDecimal.ZERO)
                );

        // Imagen principal = primera variante activa
        activas.stream()
                .findFirst()
                .map(ProductoCardResponse.VarianteCard::getImagenUrl)
                .ifPresentOrElse(
                        p::setImagenPrincipal,
                        () -> p.setImagenPrincipal("https://placehold.co/800x800/eeeeee/999999.png?text=Sin+Imagen")
                );
    }

    // ===================================================================
    // MAPPER PRIVADO (opcional, si usas ProductoResponse en admin)
    // ===================================================================

    private ProductoResponse toResponse(Producto p) {
        // Tu mapper actual de admin → lo dejas igual
        ProductoResponse resp = new ProductoResponse();
        resp.setId(p.getId());
        resp.setNombre(p.getNombre());
        resp.setSlug(p.getSlug());
        // ... el resto de campos
        return resp;
    }
}