// src/main/java/com/proyecto/StoreCollection/service/ProductoServiceImpl.java

package com.proyecto.StoreCollection.service;

import com.cloudinary.utils.ObjectUtils;
import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.request.ProductoRequest;
import com.proyecto.StoreCollection.dto.request.VarianteRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.dto.response.ProductoCardResponse;
import com.proyecto.StoreCollection.dto.response.ProductoResponse;
import com.proyecto.StoreCollection.dto.response.VarianteResponse;
import com.proyecto.StoreCollection.entity.*;
import com.proyecto.StoreCollection.repository.*;
import com.proyecto.StoreCollection.service.Cloudinary.CloudinaryService;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    private final CloudinaryService cloudinaryService;

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
        if (variantesRequests == null || variantesRequests.isEmpty()) {
            varianteRepository.deleteAll(producto.getVariantes());
            producto.getVariantes().clear();
            return;
        }

        Set<Integer> idsRequest = variantesRequests.stream()
                .map(VarianteRequest::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ProductoVariante> aEliminar = producto.getVariantes().stream()
                .filter(v -> !idsRequest.contains(v.getId()))
                .toList();

        if (!aEliminar.isEmpty()) {
            // Opcional: eliminar imágenes antiguas de Cloudinary
            aEliminar.forEach(v -> {
                if (v.getImagenUrl() != null && !v.getImagenUrl().contains("placehold.co")) {
                    try {
                        String publicId = extractPublicId(v.getImagenUrl());
                        if (publicId != null) {
                            cloudinaryService.delete(publicId);
                        }
                    } catch (Exception e) {
                        // Loggear pero no fallar la operación
                        System.err.println("No se pudo eliminar imagen de Cloudinary: " + e.getMessage());
                    }
                }
            });
            varianteRepository.deleteAll(aEliminar);
        }

        producto.getVariantes().removeAll(aEliminar);

        for (VarianteRequest req : variantesRequests) {
            ProductoVariante variante;

            if (req.getId() != null) {
                variante = varianteRepository.findById(req.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Variante con ID " + req.getId() + " no encontrada."));
            } else {
                variante = new ProductoVariante();
                variante.setProducto(producto);
            }

            variante.setTienda(tienda);
            variante.setSku(req.getSku().trim());
            variante.setPrecio(req.getPrecio());
            variante.setStock(req.getStock() != null ? req.getStock() : 0);
            variante.setActivo(req.getActivo() != null ? req.getActivo() : true);

            // === SUBIDA DE IMAGEN A CLOUDINARY ===
            String imagenUrlFinal = null;

            // Caso 1: Se subió un nuevo archivo
            if (req.getImagen() != null && !req.getImagen().isEmpty()) {
                try {
                    Map uploadResult = cloudinaryService.upload(
                            req.getImagen(),
                            ObjectUtils.asMap(
                                    "folder", "tiendas/" + tienda.getSlug() + "/productos",
                                    "use_filename", false,
                                    "unique_filename", true,
                                    "overwrite", true
                            )
                    );

                    String publicId = (String) uploadResult.get("public_id");
                    // URL optimizada: 800x800, auto calidad y formato
                    imagenUrlFinal = cloudinaryService.getResizedUrl(publicId, 800, 800);

                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error al subir imagen a Cloudinary: " + e.getMessage());
                }
            }
            // Caso 2: Ya tenía imagen y no se cambió → mantener la existente
            else if (req.getImagenUrl() != null && !req.getImagenUrl().isEmpty()
                    && !req.getImagenUrl().contains("placehold.co")) {
                imagenUrlFinal = req.getImagenUrl();
            }
            // Caso 3: Sin imagen → placeholder
            else {
                imagenUrlFinal = "https://placehold.co/800x800/eeeeee/999999.png?text=Sin+Imagen";
            }

            variante.setImagenUrl(imagenUrlFinal);

            manejarAtributosValores(variante, req.getAtributos(), tienda);
            producto.getVariantes().add(variante);
        }
    }

    private String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) return null;
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;
            String afterUpload = parts[1];
            // Quitar versión si existe (v123456789/)
            int versionIndex = afterUpload.indexOf("/v");
            if (versionIndex != -1) {
                afterUpload = afterUpload.substring(versionIndex + 1);
                int slashAfterVersion = afterUpload.indexOf("/");
                if (slashAfterVersion != -1) {
                    afterUpload = afterUpload.substring(slashAfterVersion + 1);
                }
            }
            // Quitar extensión
            return afterUpload.replaceAll("\\.[a-zA-Z0-9]+$", "");
        } catch (Exception e) {
            return null;
        }
    }

    private void manejarAtributosValores(ProductoVariante variante, List<AtributoValorRequest> atributosRequests, Tienda tienda) {
        if (atributosRequests == null || atributosRequests.isEmpty()) {
            variante.getAtributos().clear();
            return;
        }

        variante.getAtributos().clear();

        for (AtributoValorRequest req : atributosRequests) {
            String nombreAtributo = req.getAtributoNombre().trim();
            String valorStr = req.getValor().trim();

            if (nombreAtributo.isEmpty() || valorStr.isEmpty()) {
                continue; // o throw si prefieres validar fuerte
            }

            // Buscar o crear Atributo
            Atributo atributo = atributoRepository.findByNombreAndTiendaId(nombreAtributo, tienda.getId())
                    .orElseGet(() -> {
                        Atributo nuevo = new Atributo();
                        nuevo.setNombre(nombreAtributo);
                        nuevo.setTienda(tienda);
                        return atributoRepository.save(nuevo);
                    });

            // REUTILIZAR AtributoValor si ya existe (evita duplicados y errores de tienda_id)
            AtributoValor atributoValor = atributoValorRepository
                    .findByAtributoIdAndValor(atributo.getId(), valorStr)
                    .orElseGet(() -> {
                        AtributoValor nuevo = new AtributoValor();
                        nuevo.setAtributo(atributo);
                        nuevo.setTienda(tienda);
                        nuevo.setValor(valorStr);
                        return atributoValorRepository.save(nuevo);
                    });

            variante.getAtributos().add(atributoValor);
        }
    }

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

        // ← Aquí está la magia: devolvemos TODO
        return toResponseProductoCreate(producto);   // ← ya incluye variantes + atributos + valores
    }
    @Override
    @Transactional
    public ProductoResponse toggleActivo(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No encontrado"));

        boolean nuevoEstado = !producto.isActivo();
        producto.setActivo(nuevoEstado);
        productoRepository.save(producto); // Guardamos el estado del producto

        if (nuevoEstado) {
            varianteRepository.activarTodasPorProductoId(id);
        } else {
            varianteRepository.desactivarTodasPorProductoId(id);
        }

        // FORZAR RELECTURA: Limpiamos la caché de primer nivel y buscamos de nuevo
        productoRepository.flush();

        // Volvemos a buscar el producto para que toResponse() tenga los datos reales de la BD
        Producto productoActualizado = productoRepository.findById(id).get();

        return toResponse(productoActualizado);
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
            Boolean productoActivo = (Boolean) row[8]; // Asegúrate de que tu query incluya producto.activo

            // ← NUEVO: Filtrar productos inactivos desde el principio
            if (productoActivo == null || !productoActivo) {
                continue; // Saltar todo el producto si está inactivo
            }

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

        return map.values().stream()
                .peek(this::calcularCamposDerivados)
                .filter(p -> p.getStockTotal() > 0)
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

        Boolean productoActivo = (Boolean) rows.get(0)[9];
        if (productoActivo == null || !productoActivo) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El producto no está disponible.");
        }

        // Mapa: productoId → respuesta
        Map<Integer, ProductoCardResponse> productoMap = new LinkedHashMap<>();
        // Mapa auxiliar: varianteId → lista de atributos
        Map<Integer, List<ProductoCardResponse.VarianteCard.AtributoValorDTO>> atributosMap = new HashMap<>();

        for (Object[] row : rows) {
            Integer varianteId = (Integer) row[0];
            Integer productoId = (Integer) row[1];
            String nombre = (String) row[2];
            String slug = (String) row[3];
            String nombreCategoria = (String) row[4];
            BigDecimal precio = (BigDecimal) row[5];
            Integer stock = (Integer) row[6];
            String imagenUrl = (String) row[7];
            Boolean activoVar = (Boolean) row[8];

            // Crear producto base
            ProductoCardResponse p = productoMap.computeIfAbsent(productoId, k -> {
                ProductoCardResponse dto = new ProductoCardResponse();
                dto.setId(productoId);
                dto.setNombre(nombre);
                dto.setSlug(slug);
                dto.setNombreCategoria(nombreCategoria);
                dto.setVariantes(new ArrayList<>());
                return dto;
            });

            // Solo procesar variantes activas con precio
            if (activoVar && precio != null) {
                // Buscar o crear variante
                ProductoCardResponse.VarianteCard variante = p.getVariantes().stream()
                        .filter(v -> v.getId() != null && v.getId().equals(varianteId))
                        .findFirst()
                        .orElseGet(() -> {
                            ProductoCardResponse.VarianteCard v = new ProductoCardResponse.VarianteCard();
                            v.setId(varianteId);
                            v.setPrecio(precio);
                            v.setStock(stock);
                            v.setImagenUrl(imagenUrl);
                            v.setActivo(true);
                            p.getVariantes().add(v);
                            return v;
                        });

                // Agregar atributo si existe
                String atributoNombre = (String) row[10];
                String valor = (String) row[11];
                if (atributoNombre != null && valor != null) {
                    ProductoCardResponse.VarianteCard.AtributoValorDTO attr = new ProductoCardResponse.VarianteCard.AtributoValorDTO();
                    attr.setAtributoNombre(atributoNombre);
                    attr.setValor(valor);
                    variante.getAtributos().add(attr);
                }
            }
        }

        ProductoCardResponse resultado = productoMap.values().iterator().next();
        calcularCamposDerivados(resultado);

        if (resultado.getStockTotal() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El producto no tiene stock disponible.");
        }

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
    public List<DropTownStandar> getProductosForDropdown() {
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
                    DropTownStandar dto = new DropTownStandar();
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
        resp.setActivo(p.isActivo());
        return resp;
    }
    private ProductoResponse toResponseProductoCreate(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setId(producto.getId());
        response.setNombre(producto.getNombre());
        response.setSlug(producto.getSlug());
        response.setCategoriaId(producto.getCategoria().getId());
        response.setCategoriaNombre(producto.getCategoria().getNombre());
        response.setTiendaId(producto.getTienda().getId());
        response.setActivo(producto.isActivo());

        // Variantes + atributos
        List<VarianteResponse> variantesResponse = producto.getVariantes().stream()
                .map(v -> {
                    VarianteResponse vr = new VarianteResponse();
                    vr.setId(v.getId());
                    vr.setSku(v.getSku());
                    vr.setPrecio(v.getPrecio());
                    vr.setStock(v.getStock());
                    vr.setImagenUrl(v.getImagenUrl());
                    vr.setActivo(v.isActivo());

                    // Atributos de esta variante
                    List<AtributoValorResponse> attrs = v.getAtributos().stream()
                            .map(av -> {
                                AtributoValorResponse avr = new AtributoValorResponse();
                                avr.setId(av.getId());
                                avr.setAtributoNombre(av.getAtributo().getNombre());
                                avr.setValor(av.getValor());
                                return avr;
                            })
                            .collect(Collectors.toList());

                    vr.setAtributos(attrs);
                    return vr;
                })
                .collect(Collectors.toList());

        response.setVariantes(variantesResponse);
        return response;
    }
}