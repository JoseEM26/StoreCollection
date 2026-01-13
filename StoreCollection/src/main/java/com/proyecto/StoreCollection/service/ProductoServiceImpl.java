    package com.proyecto.StoreCollection.service;
    
    import com.cloudinary.utils.ObjectUtils;
    import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
    import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
    import com.proyecto.StoreCollection.dto.request.ProductoRequest;
    import com.proyecto.StoreCollection.dto.request.VarianteRequest;
    import com.proyecto.StoreCollection.dto.response.*;
    import com.proyecto.StoreCollection.dto.special.ProductoAdminListDTO;
    import com.proyecto.StoreCollection.entity.*;
    import com.proyecto.StoreCollection.repository.*;
    import com.proyecto.StoreCollection.service.Cloudinary.CloudinaryService;
    import com.proyecto.StoreCollection.tenant.TenantContext;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.http.HttpStatus;
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
    
        // ======================== VALIDACIONES CON MENSAJES CLAROS ========================
    

    
        private void validarLimiteProductos(Tienda tienda) {
            long conteoActual = productoRepository.countByTiendaIdAndActivoTrue(tienda.getId());
            int maxPermitido = tienda.getPlan().getMaxProductos();
    
            if (conteoActual >= maxPermitido) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "¡Límite de productos alcanzado! Tu plan '" + tienda.getPlan().getNombre() +
                                "' permite un máximo de " + maxPermitido + " productos y ya tienes " + conteoActual +
                                ". Para agregar más, actualiza a un plan superior.");
            }
        }
    
        private void validarLimiteVariantes(Tienda tienda, List<VarianteRequest> requests, Integer productoIdActual) {
            long variantesExistentes = varianteRepository.countByTiendaId(tienda.getId());
    
            if (productoIdActual != null) {
                variantesExistentes -= varianteRepository.countByProductoId(productoIdActual);
            }
    
            long variantesNuevas = requests.stream()
                    .filter(req -> req.getId() == null)
                    .count();
    
            int maxPermitido = tienda.getPlan().getMaxVariantes();
    
            if (variantesExistentes + variantesNuevas > maxPermitido) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "¡Límite de variantes excedido! Tu plan '" + tienda.getPlan().getNombre() +
                                "' permite máximo " + maxPermitido + " variantes en total. Actualmente tienes " +
                                variantesExistentes + " y estás intentando agregar " + variantesNuevas +
                                " más. Actualiza tu plan para tener más variantes.");
            }
        }
    
        // ======================== MÉTODOS PRINCIPALES ========================
    
        @Override
        public Page<ProductoResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable) {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) return Page.empty(pageable);
    
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
            return productoRepository.findAll(pageable).map(this::toResponse);
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
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    
            Producto producto;
            Tienda tiendaAsignada;
    
            if (id == null) {
                // ==================== CREACIÓN ====================
                producto = new Producto();
    
                if (esAdmin) {
                    if (request.getTiendaId() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'tiendaId' es obligatorio cuando eres administrador.");
                    }
                    tiendaAsignada = tiendaService.getEntityById(request.getTiendaId());
                } else {
                    tiendaAsignada = tiendaService.getTiendaDelUsuarioActual();
                }
    
                validarLimiteProductos(tiendaAsignada);
    
                if (productoRepository.findBySlugAndTiendaId(request.getSlug(), tiendaAsignada.getId()).isPresent()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "¡Slug duplicado! Ya existe un producto con el slug '" + request.getSlug() +
                                    "' en tu tienda. Por favor, elige uno diferente.");
                }
            } else {
                // ==================== EDICIÓN ====================
                producto = productoRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Producto no encontrado. Es posible que haya sido eliminado."));
    
                tiendaAsignada = producto.getTienda();

                Optional<Producto> otro = productoRepository.findBySlugAndTiendaId(request.getSlug(), tiendaAsignada.getId());
                if (otro.isPresent() && !otro.get().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "¡Slug en uso! Ya existe otro producto con el slug '" + request.getSlug() +
                                    "'. Usa un slug único para cada producto.");
                }
            }
    
            // Validar variantes si se envían
            if (request.getVariantes() != null && !request.getVariantes().isEmpty()) {
                validarLimiteVariantes(tiendaAsignada, request.getVariantes(), id);
            }
    
            // ==================== DATOS BÁSICOS ====================
            producto.setNombre(request.getNombre().trim());
            producto.setSlug(request.getSlug().trim());

            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Categoría no encontrada. Verifica que la categoría exista en tu tienda."));
    
            if (!categoria.getTienda().getId().equals(tiendaAsignada.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "¡Error de seguridad! La categoría seleccionada no pertenece a tu tienda.");
            }
    
            producto.setCategoria(categoria);
            producto.setTienda(tiendaAsignada);
    
            // ==================== VARIANTEs ====================
            manejarVariantes(producto, request.getVariantes(), tiendaAsignada);
    
            // ==================== GUARDAR ====================
            Producto saved = productoRepository.save(producto);
            return toResponse(saved);
        }
    
        // ==================== MANEJO DE VARIANTEs (sin cambios importantes) ====================
    
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
                aEliminar.forEach(v -> {
                    if (v.getImagenUrl() != null && !v.getImagenUrl().contains("placehold.co")) {
                        try {
                            String publicId = extractPublicId(v.getImagenUrl());
                            if (publicId != null) {
                                cloudinaryService.delete(publicId);
                            }
                        } catch (Exception e) {
                            System.err.println("Error eliminando imagen Cloudinary: " + e.getMessage());
                        }
                    }
                });
                varianteRepository.deleteAll(aEliminar);
            }
    
            producto.getVariantes().removeAll(aEliminar);
    
            for (VarianteRequest req : variantesRequests) {
                ProductoVariante variante = req.getId() != null
                        ? varianteRepository.findById(req.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Variante con ID " + req.getId() + " no encontrada. Refresca la página e inténtalo de nuevo."))
                        : new ProductoVariante();
    
                variante.setProducto(producto);
                variante.setTienda(tienda);
                variante.setSku(req.getSku().trim());
                variante.setPrecio(req.getPrecio());
                variante.setDescripcion_corta(req.getDescripcion_corta());
                variante.setPrecio_anterior(req.getPrecio_anterior());
                variante.setStock(req.getStock() != null ? req.getStock() : 0);
                variante.setActivo(req.getActivo() != null ? req.getActivo() : true);
    
                String imagenUrlFinal;
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
                        imagenUrlFinal = cloudinaryService.getResizedUrl(publicId, 800, 800);
                    } catch (IOException e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Error al subir la imagen a Cloudinary. Inténtalo de nuevo o usa una imagen existente.");
                    }
                } else if (req.getImagenUrl() != null && !req.getImagenUrl().isEmpty()
                        && !req.getImagenUrl().contains("placehold.co")) {
                    imagenUrlFinal = req.getImagenUrl();
                } else {
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
                String after = parts[1];
                int vIndex = after.indexOf("/v");
                if (vIndex != -1) {
                    after = after.substring(vIndex + 1);
                    int slash = after.indexOf("/");
                    if (slash != -1) after = after.substring(slash + 1);
                }
                return after.replaceAll("\\.[a-zA-Z0-9]+$", "");
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
                String nombre = req.getAtributoNombre().trim();
                String valor = req.getValor().trim();
                if (nombre.isEmpty() || valor.isEmpty()) continue;
    
                Atributo atributo = atributoRepository.findByNombreAndTiendaId(nombre, tienda.getId())
                        .orElseGet(() -> {
                            Atributo nuevo = new Atributo();
                            nuevo.setNombre(nombre);
                            nuevo.setTienda(tienda);
                            return atributoRepository.save(nuevo);
                        });
    
                AtributoValor atributoValor = atributoValorRepository.findByAtributoIdAndValor(atributo.getId(), valor)
                        .orElseGet(() -> {
                            AtributoValor nuevo = new AtributoValor();
                            nuevo.setAtributo(atributo);
                            nuevo.setTienda(tienda);
                            nuevo.setValor(valor);
                            return atributoValorRepository.save(nuevo);
                        });
    
                variante.getAtributos().add(atributoValor);
            }
        }
    
        // ==================== RESTO DE MÉTODOS (sin cambios mayores) ====================
    
        @Override
        @Transactional(readOnly = true)
        public ProductoResponse getProductoByIdParaEdicion(Integer id) {
            Producto producto = productoRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            return toResponseProductoCreate(producto);
        }

        @Override
        @Transactional
        public ProductoResponse toggleActivo(Integer id) {
            Producto producto = productoRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

            boolean vaAActivarse = !producto.isActivo();

            // ← Aquí está el cambio importante
            if (vaAActivarse) {
                long conteoActual = productoRepository.countByTiendaIdAndActivoTrue(producto.getTienda().getId());
                int maxPermitido = producto.getTienda().getPlan().getMaxProductos();

                if (conteoActual >= maxPermitido) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "¡No puedes activar este producto! Has alcanzado el límite de productos activos " +
                                    "de tu plan (" + maxPermitido + "). Actualmente tienes " + conteoActual + " activos. " +
                                    "Desactiva alguno o actualiza tu plan.");
                }
            }

            producto.setActivo(vaAActivarse);
            productoRepository.save(producto);

            if (vaAActivarse) {
                varianteRepository.activarTodasPorProductoId(id);
            } else {
                varianteRepository.desactivarTodasPorProductoId(id);
            }

            return toResponse(productoRepository.findById(id).get());
        }
        @Override
        public void deleteById(Integer id) {
            productoRepository.delete(productoRepository.getByIdAndTenant(id));
        }
    
        @Override
        public Page<ProductoResponse> findByUserEmail(String email, Pageable pageable) {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) return Page.empty(pageable);
            return productoRepository.findAllByTiendaId(tenantId, pageable).map(this::toResponse);
        }
        @Override
        @Transactional(readOnly = true)
        public List<ProductoCardResponse> findAllForPublicCatalog(String tiendaSlug) {
            List<Object[]> rows = productoRepository.findRawCatalogByTiendaSlug(tiendaSlug);
            Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

            for (Object[] row : rows) {
                Integer productoId = (Integer) row[0];
                Boolean productoActivo = (Boolean) row[8];

                if (productoActivo == null || !productoActivo) {
                    continue;
                }

                ProductoCardResponse p = map.computeIfAbsent(productoId, k -> {
                    ProductoCardResponse dto = new ProductoCardResponse();
                    dto.setId(productoId);
                    dto.setNombre((String) row[1]);
                    dto.setSlug((String) row[2]);
                    dto.setNombreCategoria((String) row[3]);
                    dto.setVariantes(new ArrayList<>());
                    return dto;
                });

                BigDecimal precio = row[4] != null ? new BigDecimal(row[4].toString()) : null;
                Integer stock     = row[5] != null ? (Integer) row[5] : 0;
                String imagenUrl  = (String) row[6];
                Boolean varianteActivo = row[7] != null ? (Boolean) row[7] : false;

                if (varianteActivo && precio != null) {
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

            Map<Integer, ProductoCardResponse> map = new LinkedHashMap<>();

            for (Object[] row : rows) {
                Integer varianteId     = (Integer) row[0];
                Integer productoId     = (Integer) row[1];
                String nombre          = (String)  row[2];
                String slug            = (String)  row[3];
                String nombreCategoria = (String)  row[4];

                // ── Corrección importante aquí ────────────────────────────
                BigDecimal precio      = row[5] != null ? new BigDecimal(row[5].toString()) : null;
                //                                                   ↑↑↑↑↑
                //                                                   ¡Ahora sí es el índice correcto!

                Integer stock          = row[6] != null ? (Integer) row[6] : 0;
                String imagenUrl       = (String)  row[7];
                Boolean varianteActivo = row[8] != null ? (Boolean) row[8] : false;

                ProductoCardResponse p = map.computeIfAbsent(productoId, k -> {
                    ProductoCardResponse dto = new ProductoCardResponse();
                    dto.setId(productoId);
                    dto.setNombre(nombre);
                    dto.setSlug(slug);
                    dto.setNombreCategoria(nombreCategoria);
                    dto.setVariantes(new ArrayList<>());
                    return dto;
                });

                if (varianteActivo && precio != null) {
                    ProductoCardResponse.VarianteCard variante = p.getVariantes().stream()
                            .filter(v -> Objects.equals(v.getId(), varianteId))
                            .findFirst()
                            .orElseGet(() -> {
                                ProductoCardResponse.VarianteCard v = new ProductoCardResponse.VarianteCard();
                                v.setId(varianteId);
                                v.setPrecio(precio);
                                v.setStock(stock);
                                v.setImagenUrl(imagenUrl);
                                v.setActivo(true);
                                v.setPrecio_anterior(null);
                                v.setDescripcion_corta(null);
                                v.setAtributos(new ArrayList<>());
                                p.getVariantes().add(v);
                                return v;
                            });

                    BigDecimal precioAnterior = row[10] != null ? new BigDecimal(row[10].toString()) : null;
                    String descCorta = (String) row[11];
                    variante.setPrecio_anterior(precioAnterior);
                    variante.setDescripcion_corta(descCorta);

                    String attrNombre = (String) row[12];
                    String attrValor = (String) row[13];
                    if (attrNombre != null && attrValor != null) {
                        var attr = new ProductoCardResponse.VarianteCard.AtributoValorDTO();
                        attr.setAtributoNombre(attrNombre);
                        attr.setValor(attrValor);
                        variante.getAtributos().add(attr);
                    }
                }
            }

            ProductoCardResponse resultado = map.values().iterator().next();
            calcularCamposDerivados(resultado);

            if (resultado.getStockTotal() <= 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El producto no tiene stock disponible.");
            }

            return resultado;
        }
        private void calcularCamposDerivados(ProductoCardResponse p) {
            List<ProductoCardResponse.VarianteCard> activas = p.getVariantes().stream()
                    .filter(ProductoCardResponse.VarianteCard::isActivo)
                    .toList();
    
            int stockTotal = activas.stream().mapToInt(ProductoCardResponse.VarianteCard::getStock).sum();
            p.setStockTotal(stockTotal);
    
            activas.stream()
                    .map(ProductoCardResponse.VarianteCard::getPrecio)
                    .min(BigDecimal::compareTo)
                    .ifPresentOrElse(p::setPrecioMinimo, () -> p.setPrecioMinimo(BigDecimal.ZERO));
    
            activas.stream()
                    .findFirst()
                    .map(ProductoCardResponse.VarianteCard::getImagenUrl)
                    .ifPresentOrElse(p::setImagenPrincipal,
                            () -> p.setImagenPrincipal("https://placehold.co/800x800/eeeeee/999999.png?text=Sin+Imagen"));
        }
    
        @Override
        @Transactional(readOnly = true)
        public List<DropTownStandar> getProductosForDropdown() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    
            List<Producto> productos = esAdmin
                    ? productoRepository.findAllByOrderByNombreAsc()
                    : Optional.ofNullable(TenantContext.getTenantId())
                    .map(productoRepository::findByTiendaIdOrderByNombreAsc)
                    .orElse(Collections.emptyList());
    
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

            List<VarianteResponse> variantesResponse = producto.getVariantes().stream()
                    .map(v -> {
                        VarianteResponse vr = new VarianteResponse();
                        vr.setId(v.getId());
                        vr.setSku(v.getSku());
                        vr.setPrecio(v.getPrecio());
                        vr.setStock(v.getStock());
                        vr.setImagenUrl(v.getImagenUrl());
                        vr.setActivo(v.isActivo());
                        vr.setDescripcion_corta(v.getDescripcion_corta());
                        vr.setPrecio_anterior(v.getPrecio_anterior());

                        List<AtributoValorResponse> attrs = v.getAtributos().stream()
                                .map(av -> {
                                    AtributoValorResponse avr = new AtributoValorResponse();
                                    avr.setId(av.getId());
                                    avr.setAtributoNombre(av.getAtributo().getNombre());
                                    avr.setValor(av.getValor());
                                    return avr;
                                })
                                .toList();
    
                        vr.setAtributos(attrs);
                        return vr;
                    })
                    .toList();
    
            response.setVariantes(variantesResponse);
            return response;
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ProductoAdminListDTO> listarTodosAdminList(Pageable pageable) {
            return productoRepository.findAll(pageable)
                    .map(this::toAdminListDTO);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ProductoAdminListDTO> buscarTodosPorNombreAdminList(String nombre, Pageable pageable) {
            return productoRepository.findByNombreContainingIgnoreCase(nombre.trim(), pageable)
                    .map(this::toAdminListDTO);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ProductoAdminListDTO> listarPorTiendaAdminList(Integer tiendaId, Pageable pageable) {
            return productoRepository.findAllByTiendaId(tiendaId, pageable)
                    .map(this::toAdminListDTO);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ProductoAdminListDTO> buscarPorNombreYTiendaAdminList(Integer tiendaId, String nombre, Pageable pageable) {
            return productoRepository.findByNombreContainingIgnoreCaseAndTenantId(nombre.trim(), tiendaId, pageable)
                    .map(this::toAdminListDTO);
        }
    
        private ProductoAdminListDTO toAdminListDTO(Producto p) {
            var variantes = p.getVariantes() != null ? p.getVariantes() : List.<ProductoVariante>of();
            var activas = variantes.stream().filter(ProductoVariante::isActivo).toList();
    
            BigDecimal precioMin = activas.stream()
                    .map(ProductoVariante::getPrecio)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
    
            BigDecimal precioMax = activas.stream()
                    .map(ProductoVariante::getPrecio)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
    
            int stockTotal = activas.stream()
                    .mapToInt(ProductoVariante::getStock)
                    .sum();
    
            String imagenPrincipal = activas.stream()
                    .map(ProductoVariante::getImagenUrl)
                    .filter(url -> url != null && !url.contains("placehold.co"))
                    .findFirst()
                    .orElse("https://placehold.co/400x400?text=Sin+Imagen");
    
            return ProductoAdminListDTO.builder()
                    .id(p.getId())
                    .nombre(p.getNombre())
                    .slug(p.getSlug())
                    .categoriaId(p.getCategoria().getId())
                    .categoriaNombre(p.getCategoria().getNombre())
                    .tiendaId(p.getTienda().getId())
                    .tiendaNombre(p.getTienda().getNombre()) // ← importante para admin
                    .activo(p.isActivo())
                    .precioMinimo(precioMin)
                    .precioMaximo(precioMax)
                    .stockTotal(stockTotal)
                    .imagenPrincipal(imagenPrincipal)
                    .tieneVariantes(!variantes.isEmpty())
                    .cantidadVariantes(variantes.size())
                    .build();
        }
    
    }