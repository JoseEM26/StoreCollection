package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.config.AuthService;
import com.proyecto.StoreCollection.dto.request.*;
import com.proyecto.StoreCollection.dto.response.ProductoCreateResponse;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoCompletoServiceImpl {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AtributoValorRepository atributoValorRepository;
    private final TiendaService tiendaService;
    private final AuthService authService;

    // ==================== CREAR ====================
    public ProductoCreateResponse crearProductoCompleto(ProductoCompletoRequest request) {
        return guardarProductoCompleto(request, null);
    }

    // ==================== ACTUALIZAR ====================
    public ProductoCreateResponse actualizarProductoCompleto(Integer productoId, ProductoCompletoRequest request) {
        if (request.getId() != null && !request.getId().equals(productoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID del body no coincide con la URL");
        }
        request.setId(productoId);
        return guardarProductoCompleto(request, productoId);
    }

    // ==================== MÉTODO COMÚN (CREATE O UPDATE) ====================
    private ProductoCreateResponse guardarProductoCompleto(ProductoCompletoRequest request, Integer existingId) {
        boolean esAdmin = authService.isAdmin();

        // ===== DETERMINAR LA TIENDA =====
        Tienda tiendaDestino;

        if (esAdmin && request.getTiendaId() != null) {
            // ADMIN puede especificar tienda
            tiendaDestino = tiendaService.getTiendaById(request.getTiendaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tienda no encontrada"));
        } else if (esAdmin) {
            // ADMIN no especificó tienda → usa la del usuario actual (opcional)
            tiendaDestino = tiendaService.getTiendaDelUsuarioActual();
        } else {
            // OWNER: NO puede elegir tienda → siempre la suya
            tiendaDestino = tiendaService.getTiendaDelUsuarioActual();

            // OWNER intenta meter tiendaId → RECHAZAR
            if (request.getTiendaId() != null && !request.getTiendaId().equals(tiendaDestino.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes crear/editar productos en otra tienda");
            }
        }

        Integer tiendaId = tiendaDestino.getId();

        // ===== VALIDACIONES =====
        validarRequest(request, tiendaId, existingId, esAdmin);

        // ===== CATEGORÍA =====
        Categoria categoria = esAdmin
                ? categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"))
                : categoriaRepository.findByIdAndTienda(request.getCategoriaId(), tiendaDestino)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no pertenece a tu tienda"));

        // ===== PRODUCTO =====
        Producto producto;
        if (existingId == null) {
            producto = new Producto();
        } else {
            producto = productoRepository.getByIdAndTenant(existingId);

            // Seguridad extra: OWNER no puede editar producto de otra tienda
            if (!esAdmin && !producto.getTienda().getId().equals(tiendaId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para editar este producto");
            }
        }

        producto.setNombre(request.getNombre());
        producto.setSlug(request.getSlug());
        producto.setCategoria(categoria);
        producto.setTienda(tiendaDestino);

        producto = productoRepository.save(producto);

        // ===== GESTIÓN DE VARIANTES =====
        Set<Integer> idsActuales = request.getVariantes().stream()
                .map(VarianteCompletaRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        producto.getVariantes().removeIf(v -> v.getId() != null && !idsActuales.contains(v.getId()));

        for (VarianteCompletaRequest varReq : request.getVariantes()) {
            if (varReq.getId() != null && idsActuales.contains(varReq.getId())) {
                actualizarVarianteExistente(producto, varReq, tiendaDestino, esAdmin);
            } else {
                crearNuevaVariante(producto, varReq, tiendaDestino, esAdmin);
            }
        }

        return mapToResponse(producto);
    }

    private void crearNuevaVariante(Producto producto, VarianteCompletaRequest varReq, Tienda tienda, boolean esAdmin) {
        ProductoVariante variante = new ProductoVariante();
        configurarVariante(variante, producto, varReq, tienda, esAdmin);
        producto.getVariantes().add(variante);
    }

    private void actualizarVarianteExistente(Producto producto, VarianteCompletaRequest varReq, Tienda tienda, boolean esAdmin) {
        ProductoVariante variante = producto.getVariantes().stream()
                .filter(v -> v.getId().equals(varReq.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variante no encontrada"));

        configurarVariante(variante, producto, varReq, tienda, esAdmin);
    }

    private void configurarVariante(ProductoVariante variante, Producto producto, VarianteCompletaRequest varReq, Tienda tienda, boolean esAdmin) {
        variante.setProducto(producto);
        variante.setTienda(tienda);
        variante.setSku(varReq.getSku());
        variante.setPrecio(varReq.getPrecio());
        variante.setStock(varReq.getStock() != null ? varReq.getStock() : 0);
        variante.setImagenUrl(varReq.getImagenUrl());
        variante.setActivo(varReq.getActivo() != null ? varReq.getActivo() : true);

        // ATRIBUTOS
        List<AtributoValor> atributos = new ArrayList<>();
        if (varReq.getAtributoValorIds() != null && !varReq.getAtributoValorIds().isEmpty()) {
            atributos = esAdmin
                    ? atributoValorRepository.findAllById(varReq.getAtributoValorIds())
                    : atributoValorRepository.findAllByIdInAndTiendaId(varReq.getAtributoValorIds(), tienda.getId());

            if (atributos.size() != varReq.getAtributoValorIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Algunos valores de atributo no existen o no pertenecen a la tienda");
            }
        }
        variante.setAtributos(atributos);
    }

    private void validarRequest(ProductoCompletoRequest request, Integer tiendaId, Integer existingId, boolean esAdmin) {
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (request.getSlug() == null || request.getSlug().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El slug es obligatorio");
        }
        if (request.getCategoriaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría obligatoria");
        }
        if (request.getVariantes() == null || request.getVariantes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Al menos una variante requerida");
        }

        // Slug único por tienda
        boolean slugExiste = productoRepository.existsBySlugAndTiendaId(request.getSlug(), tiendaId);
        if (slugExiste && (existingId == null || !productoRepository.findById(existingId)
                .map(p -> p.getSlug().equals(request.getSlug()))
                .orElse(false))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El slug ya está en uso en esta tienda");
        }

        for (int i = 0; i < request.getVariantes().size(); i++) {
            VarianteCompletaRequest v = request.getVariantes().get(i);
            if (v.getSku() == null || v.getSku().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU requerido en variante " + (i + 1));
            }
            if (v.getPrecio() == null || v.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Precio debe ser mayor a 0 en variante " + (i + 1));
            }
        }
    }

    private ProductoCreateResponse mapToResponse(Producto p) {
        ProductoCreateResponse resp = new ProductoCreateResponse();
        resp.setId(p.getId());
        resp.setNombre(p.getNombre());
        resp.setSlug(p.getSlug());
        resp.setCategoriaId(p.getCategoria().getId());
        resp.setCategoriaNombre(p.getCategoria().getNombre());
        resp.setTiendaId(p.getTienda().getId());
        resp.setTiendaSlug(p.getTienda().getSlug());

        var activas = p.getVariantes().stream()
                .filter(ProductoVariante::getActivo)
                .toList();

        resp.setTotalVariantes(p.getVariantes().size());
        resp.setVariantesActivas(activas.size());

        activas.stream()
                .map(ProductoVariante::getPrecio)
                .min(BigDecimal::compareTo)
                .ifPresent(resp::setPrecioMinimo);

        int stock = activas.stream().mapToInt(ProductoVariante::getStock).sum();
        resp.setStockTotal(stock);

        activas.stream()
                .map(ProductoVariante::getImagenUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(resp::setImagenPrincipal);

        List<ProductoCreateResponse.VarianteSimple> vars = activas.stream().map(v -> {
            var vs = new ProductoCreateResponse.VarianteSimple();
            vs.setId(v.getId());
            vs.setSku(v.getSku());
            vs.setPrecio(v.getPrecio());
            vs.setStock(v.getStock());
            vs.setImagenUrl(v.getImagenUrl());
            vs.setActivo(v.getActivo());

            List<ProductoCreateResponse.AtributoSimple> attrs = v.getAtributos().stream()
                    .map(av -> new ProductoCreateResponse.AtributoSimple(
                            av.getAtributo().getNombre(),
                            av.getValor()
                    ))
                    .sorted(Comparator.comparing(ProductoCreateResponse.AtributoSimple::getNombreAtributo))
                    .toList();
            vs.setAtributos(attrs);
            return vs;
        }).toList();

        resp.setVariantes(vars);
        return resp;
    }
}