// src/main/java/com/proyecto/StoreCollection/service/TiendaServiceImpl.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.PlanRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.service.Cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;
    private final TiendaSuscripcionService tiendaSuscripcionService;  // ← NUEVA DEPENDENCIA

    // ======================== CONSULTAS ========================

    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findAll(Pageable pageable) {
        return tiendaRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findAllPublicasActivas(Pageable pageable) {
        LocalDateTime ahora = LocalDateTime.now();
        Page<Tienda> tiendasPage = tiendaRepository.findTiendasConSuscripcionVigente(
                ahora,
                Set.of("active", "trial", "grace"),
                pageable
        );
        return tiendasPage.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse findById(Integer id) {
        return toResponse(tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Tienda getEntityById(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        if (!esAdmin && !tienda.getUser().getEmail().equals(auth.getName())) {
            throw new AccessDeniedException("No tienes permisos para acceder a esta tienda");
        }
        return tienda;
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse findBySlug(String slug) {
        return toResponse(tiendaRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + slug)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiendaResponse> findByUserId(Integer userId) {
        return tiendaRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Tienda getTiendaDelUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Usuario no autenticado");
        }
        String email = auth.getName();
        return tiendaRepository.findFirstByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("No tienes una tienda asignada. Crea una primero."));
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse getMiTienda() {
        return toResponse(getTiendaDelUsuarioActual());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiendaResponse> getMisTiendas() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return findByUserId(usuario.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DropTownStandar> getTiendasForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Collections.emptyList();
        }

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Tienda> tiendas;
        if (esAdmin) {
            tiendas = tiendaRepository.findAllByOrderByNombreAsc();
        } else {
            Tienda tiendaActual = getTiendaDelUsuarioActual();
            tiendas = tiendaActual != null ? Collections.singletonList(tiendaActual) : Collections.emptyList();
        }

        return tiendas.stream()
                .map(t -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(t.getId());
                    dto.setDescripcion(t.getNombre());
                    return dto;
                })
                .toList();
    }

    @Override
    public Page<TiendaResponse> buscarPorNombreContainingIgnoreCase(String texto, Pageable pageable) {
        return tiendaRepository.findByNombreContainingIgnoreCase(texto, pageable)
                .map(this::toResponse);
    }

    // ======================== CREAR / ACTUALIZAR ========================

    @Override
    public TiendaResponse save(TiendaRequest request) {
        return save(request, null);
    }

    @Override
    public TiendaResponse save(TiendaRequest request, Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();

        Tienda t;

        if (id == null) {
            // CREACIÓN
            t = new Tienda();

            if (request.getUserId() == null) {
                throw new RuntimeException("userId es requerido para crear una tienda");
            }

            Usuario usuario = usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!esAdmin && !usuario.getEmail().equals(emailActual)) {
                throw new RuntimeException("No puedes crear una tienda para otro usuario");
            }

            if (tiendaRepository.findBySlug(request.getSlug()).isPresent()) {
                throw new RuntimeException("Ya existe una tienda con ese slug: " + request.getSlug());
            }

            t.setUser(usuario);
        } else {
            // EDICIÓN
            t = tiendaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!t.getUser().getEmail().equals(emailActual) && !esAdmin) {
                throw new RuntimeException("No tienes permisos para editar esta tienda");
            }

            Optional<Tienda> mismaSlug = tiendaRepository.findBySlug(request.getSlug());
            if (mismaSlug.isPresent() && !mismaSlug.get().getId().equals(id)) {
                throw new RuntimeException("Ya existe otra tienda con ese slug: " + request.getSlug());
            }
        }

        // Campos comunes
        t.setNombre(request.getNombre());
        t.setSlug(request.getSlug());
        t.setWhatsapp(request.getWhatsapp());
        t.setDescripcion(request.getDescripcion());
        t.setDireccion(request.getDireccion());
        t.setHorarios(request.getHorarios());
        t.setMapa_url(request.getMapa_url());

        if (request.getMoneda() != null && !request.getMoneda().isEmpty()) {
            t.setMoneda(Tienda.Moneda.valueOf(request.getMoneda()));
        }

        // Logo con Cloudinary
        if (request.getLogoImg() != null && !request.getLogoImg().isEmpty()) {
            try {
                Map<String, Object> options = Map.of(
                        "folder", "tiendas/logos",
                        "overwrite", true,
                        "resource_type", "image"
                );
                Map uploadResult = cloudinaryService.upload(request.getLogoImg(), options);
                String secureUrl = (String) uploadResult.get("secure_url");

                // Borrar anterior si existe
                if (t.getLogo_img_url() != null && !t.getLogo_img_url().isEmpty()) {
                    String oldPublicId = extractPublicId(t.getLogo_img_url());
                    if (oldPublicId != null) {
                        try {
                            cloudinaryService.delete(oldPublicId);
                        } catch (Exception e) {
                            System.out.println("No se pudo eliminar logo anterior: " + e.getMessage());
                        }
                    }
                }
                t.setLogo_img_url(secureUrl);
            } catch (IOException e) {
                throw new RuntimeException("Error al subir logo: " + e.getMessage());
            }
        }

        // Solo admin puede cambiar activo
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (esAdmin && request.getActivo() != null) {
            t.setActivo(request.getActivo());
        }

        Tienda saved = tiendaRepository.save(t);

        // ==== SUSCRIPCIÓN INICIAL AL CREAR NUEVA TIENDA ====
        if (id == null) {
            Plan planDefecto = planRepository.findFirstByActivoTrueAndEsVisiblePublicoTrueOrderByPrecioMensualAsc()
                    .orElseThrow(() -> new RuntimeException("No hay planes disponibles para nuevas tiendas"));

            tiendaSuscripcionService.crearSuscripcionInicial(saved.getId(), planDefecto.getId());
        }

        return toResponse(saved);
    }

    // ======================== OTROS ========================

    @Override
    public void deleteById(Integer id) {
        Tienda tienda = getEntityById(id);
        if (!tienda.getUser().getEmail().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new RuntimeException("No puedes eliminar una tienda que no es tuya");
        }
        tiendaRepository.delete(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findByUserEmail(String email, Pageable pageable) {
        return tiendaRepository.findByUserEmail(email, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public TiendaResponse toggleActivo(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Solo los administradores pueden activar/desactivar tiendas");
        }

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + id));
        tienda.setActivo(!tienda.getActivo());
        return toResponse(tiendaRepository.save(tienda));
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse getTiendaByIdParaEdicion(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!tienda.getUser().getEmail().equals(emailActual) && !esAdmin) {
            throw new RuntimeException("No tienes permisos para editar esta tienda");
        }
        return toResponse(tienda);
    }

    // ======================== UTILIDADES ========================

    private String extractPublicId(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            String path = url.substring(uploadIndex + 8);
            int versionIndex = path.indexOf("/");
            if (versionIndex != -1) path = path.substring(versionIndex + 1);
            int dotIndex = path.lastIndexOf(".");
            if (dotIndex != -1) path = path.substring(0, dotIndex);
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    private TiendaResponse toResponse(Tienda t) {
        TiendaResponse dto = new TiendaResponse();
        dto.setId(t.getId());
        dto.setNombre(t.getNombre());
        dto.setSlug(t.getSlug());
        dto.setWhatsapp(t.getWhatsapp());
        dto.setMoneda(t.getMoneda().name());
        dto.setDescripcion(t.getDescripcion());
        dto.setDireccion(t.getDireccion());
        dto.setHorarios(t.getHorarios());
        dto.setMapa_url(t.getMapa_url());
        dto.setLogo_img_url(t.getLogo_img_url());
        dto.setActivo(t.getActivo());
        dto.setUserId(t.getUser().getId());
        dto.setUserEmail(t.getUser().getEmail());
        return dto;
    }
}