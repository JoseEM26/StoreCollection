// src/main/java/com/proyecto/StoreCollection/service/ProductoServiceImpl.java

package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.request.VarianteRequest;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ProductoVarianteRepository varianteRepository;
    private final AtributoRepository atributoRepository;
    private final AtributoValorRepository atributoValorRepository;


    @Override
    public Page<ProductoResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable) {
        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Page.empty(pageable);
        }
        return productoRepository.findByNombreContainingIgnoreCaseAndTenantId(nombre.trim(), tenantId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<ProductoResponse> buscarPorNombreContainingIgnoreCase(String nombre, Pageable pageable) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<ProductoResponse> findAll(Pageable pageable) {
        Page<ProductoResponse> x=productoRepository.findAll(pageable).map(this::toResponse);
        return x;
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
    @Transactional
    public ProductoResponse save(ProductoRequest request, Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Producto producto;
        Tienda tiendaAsignada;

        if (id == null) {
            // CREACIÓN
            producto = new Producto();
            if (esAdmin) {
                if (request.getTiendaId() == null) {
                    throw new RuntimeException("tiendaId requerido para ADMIN");
                }
                tiendaAsignada = tiendaService.getEntityById(request.getTiendaId());
            } else {
                tiendaAsignada = tiendaService.getTiendaDelUsuarioActual();
            }
            // Validar unicidad slug en la tienda
            if (productoRepository.findBySlugAndTiendaId(request.getSlug(), tiendaAsignada.getId()).isPresent()) {
                throw new RuntimeException("Slug duplicado en esta tienda: " + request.getSlug());
            }
        } else {
            // EDICIÓN
            producto = productoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            // Verificar permisos
            if (!esAdmin && !producto.getTienda().getUser().getEmail().equals(emailActual)) {
                throw new AccessDeniedException("No tienes permisos para editar este producto");
            }
            tiendaAsignada = producto.getTienda();  // Mantener la misma tienda
            // Validar unicidad slug excluyendo este producto
            Optional<Producto> otroConSlug = productoRepository.findBySlugAndTiendaId(request.getSlug(), tiendaAsignada.getId());
            if (otroConSlug.isPresent() && !otroConSlug.get().getId().equals(id)) {
                throw new RuntimeException("Slug duplicado en esta tienda: " + request.getSlug());
            }
        }

        // Setear campos básicos
        producto.setNombre(request.getNombre().trim());
        producto.setSlug(request.getSlug().trim());
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        // Validar que la categoría pertenezca a la misma tienda
        if (!categoria.getTienda().getId().equals(tiendaAsignada.getId())) {
            throw new RuntimeException("Categoría no pertenece a esta tienda");
        }
        producto.setCategoria(categoria);
        producto.setTienda(tiendaAsignada);

        // Manejar variantes (crear/editar/eliminar)
        manejarVariantes(producto, request.getVariantes(), tiendaAsignada);

        // Guardar y retornar
        return toResponse(productoRepository.save(producto));
    }
    private void manejarVariantes(Producto producto, List<VarianteRequest> variantesRequests, Tienda tienda) {
        if (variantesRequests == null) return;

        // Mapear variantes existentes para edición/eliminación
        Set<ProductoVariante> variantesExistentes = new HashSet<>(producto.getVariantes());
        producto.getVariantes().clear();

        for (VarianteRequest req : variantesRequests) {
            ProductoVariante variante;
            if (req.getId() != null) {
                // Edición: buscar existente
                variante = variantesExistentes.stream()
                        .filter(v -> v.getId().equals(req.getId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + req.getId()));
                variantesExistentes.remove(variante);  // Quitar de existentes (no se elimina)
            } else {
                // Creación
                variante = new ProductoVariante();
            }

            variante.setSku(req.getSku().trim());
            variante.setPrecio(req.getPrecio());
            variante.setStock(req.getStock());
            variante.setImagenUrl(req.getImagenUrl());
            variante.setProducto(producto);
            variante.setTienda(tienda);  // Asignar tienda

            // Manejar atributos/valores
            manejarAtributosValores(variante, req.getAtributos(), tienda);

            producto.getVariantes().add(variante);
        }

        // Eliminar variantes que no vinieron en el request
        variantesExistentes.forEach(varianteRepository::delete);
    }

    private void manejarAtributosValores(ProductoVariante variante, List<AtributoValorRequest> atributosRequests, Tienda tienda) {
        if (atributosRequests == null) return;

        variante.getAtributos().clear();

        for (AtributoValorRequest req : atributosRequests) {
            // Buscar o crear Atributo
            Atributo atributo = atributoRepository.findByNombreAndTiendaId(req.getAtributoNombre().trim(), tienda.getId())
                    .orElseGet(() -> {
                        Atributo nuevo = new Atributo();
                        nuevo.setNombre(req.getAtributoNombre().trim());
                        nuevo.setTienda(tienda);
                        return atributoRepository.save(nuevo);
                    });

            // Buscar o crear AtributoValor
            AtributoValor valor = atributoValorRepository.findByAtributoIdAndValor(atributo.getId(), req.getValor().trim())
                    .orElseGet(() -> {
                        AtributoValor nuevo = new AtributoValor();
                        nuevo.setAtributo(atributo);
                        nuevo.setTienda(tienda);
                        nuevo.setValor(req.getValor().trim());
                        return atributoValorRepository.save(nuevo);
                    });

            variante.getAtributos().add(valor);
        }
    }

    // Método para obtener producto para edición (con verificación)
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoByIdParaEdicion(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!esAdmin && !producto.getTienda().getUser().getEmail().equals(emailActual)) {
            throw new AccessDeniedException("No tienes permisos para acceder a este producto");
        }

        return toResponse(producto);
    }
    @Override
    @Transactional
    public ProductoResponse toggleActivo(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Solo ADMIN puede togglear el estado activo
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden activar o desactivar productos");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        // Toggle del estado activo del producto
        boolean nuevoEstado = !producto.isActivo();
        producto.setActivo(nuevoEstado);

        // Si se DESACTIVA el producto → desactivar todas sus variantes
        if (!nuevoEstado) {
            varianteRepository.desactivarTodasPorProductoId(id);
        }
        // Nota: No reactivamos variantes automáticamente al activar el producto
        // (el owner podría haber desactivado alguna variante manualmente)

        Producto saved = productoRepository.save(producto);

        return toResponse(saved);
    }
    @Override
    public void deleteById(Integer id) {
        productoRepository.delete(productoRepository.getByIdAndTenant(id));
    }

    @Override
    public Page<ProductoResponse> findByUserEmail(String email, Pageable pageable) {
        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return Page.empty(pageable);
        }
        return productoRepository.findAllByTenantId(tenantId, pageable)
                .map(this::toResponse);
    }

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
    @Override
    @Transactional(readOnly = true)
    public List<DropDownStandard> getProductosForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Producto> productos;

        if (esAdmin) {
            // ADMIN ve todos los productos
            productos = productoRepository.findAllByOrderByNombreAsc();
        } else {
            // OWNER solo ve los de su tienda
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            productos = productoRepository.findByTiendaIdOrderByNombreAsc(tenantId);
        }

        // Convertir a DTO estándar
        return productos.stream()
                .map(p -> {
                    DropDownStandard dto = new DropDownStandard();
                    dto.setId(p.getId());
                    dto.setDescripcion(p.getNombre());
                    return dto;
                })
                .toList();
    }
    private ProductoResponse toResponse(Producto p) {
        ProductoResponse resp = new ProductoResponse();
        resp.setId(p.getId());
        resp.setNombre(p.getNombre());
        resp.setSlug(p.getSlug());
        resp.setCategoriaId(p.getCategoria().getId());
        resp.setTiendaId(p.getTienda().getId());
        resp.setCategoriaNombre(p.getCategoria().getNombre());
        resp.setActivo(p.getCategoria().isActivo());
        return resp;
    }
}