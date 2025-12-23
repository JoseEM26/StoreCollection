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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;
    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findAll(Pageable pageable) {
        return tiendaRepository.findAll(pageable).map(this::toResponse);
    }
// En TiendaServiceImpl.java
@Override
@Transactional(readOnly = true)
public Page<TiendaResponse> findAllPublicasActivas(Pageable pageable) {
    // Obtener todas las tiendas activas
    List<Tienda> todasActivas = tiendaRepository.findByActivoTrue();

    int mesActual = LocalDate.now().getMonthValue();

    // Filtrar por plan vigente y mapear a Response
    List<TiendaResponse> todasFiltradas = todasActivas.stream()
            .filter(tienda -> {
                Plan plan = tienda.getPlan();
                if (plan == null || !plan.getActivo()) {
                    return false;
                }
                int inicio = plan.getMesInicio();
                int fin = plan.getMesFin();

                boolean vigente = (inicio <= fin)
                        ? (mesActual >= inicio && mesActual <= fin)
                        : (mesActual >= inicio || mesActual <= fin);

                return vigente;
            })
            .map(this::toResponse)
            .collect(Collectors.toList());

    // Ordenar siempre por nombre (asc)
    todasFiltradas.sort(Comparator.comparing(TiendaResponse::getNombre, String.CASE_INSENSITIVE_ORDER));

    long total = todasFiltradas.size();

    // Si es unpaged (usado en búsqueda global), devolver todo
    if (pageable.isUnpaged()) {
        return new PageImpl<>(todasFiltradas, pageable, total);
    }

    // Caso normal: aplicar paginación
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), todasFiltradas.size());

    List<TiendaResponse> paginaActual = todasFiltradas.subList(start, end);

    return new PageImpl<>(paginaActual, pageable, total);
}
    @Override
    @Transactional(readOnly = true)
    public TiendaResponse findById(Integer id) {
        return toResponse(tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + id)));
    }
    @Override
    public Tienda getEntityById(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // Opcional: si quieres restringir que solo ADMIN acceda a cualquier tienda
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

    // MÉTODO CLAVE: obtiene la tienda del usuario logueado
    @Override
    public Tienda getTiendaDelUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Usuario no autenticado");
        }

        String email = auth.getName(); // JWT usa email como username

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
    public Page<TiendaResponse> buscarPorNombreContainingIgnoreCase(String texto, Pageable pageable) {
        return tiendaRepository.findByNombreContainingIgnoreCase(texto, pageable)
                .map(tienda -> this.toResponse(tienda));
    }

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
            // CREACIÓN DE NUEVA TIENDA
            t = new Tienda();

            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (request.getUserId() == null) {
                throw new RuntimeException("userId es requerido para crear una tienda");
            }

            if (tiendaRepository.findBySlug(request.getSlug()).isPresent()) {
                throw new RuntimeException("Ya existe una tienda con ese slug: " + request.getSlug());
            }

            Usuario usuario = usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!esAdmin && !usuario.getEmail().equals(emailActual)) {
                throw new RuntimeException("No puedes crear una tienda para otro usuario");
            }

            t.setUser(usuario);
        } else {
            // EDICIÓN DE TIENDA EXISTENTE
            t = tiendaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            Optional<Tienda> tiendaConMismoSlug = tiendaRepository.findBySlug(request.getSlug());
            if (tiendaConMismoSlug.isPresent() && !tiendaConMismoSlug.get().getId().equals(id)) {
                throw new RuntimeException("Ya existe otra tienda con ese slug: " + request.getSlug());
            }

            if (!t.getUser().getEmail().equals(emailActual) && !esAdmin) {
                throw new RuntimeException("No tienes permisos para editar esta tienda");
            }
        }

        // === CAMPOS COMUNES ===
        t.setNombre(request.getNombre());
        t.setSlug(request.getSlug());
        t.setWhatsapp(request.getWhatsapp());

        if (request.getMoneda() != null && !request.getMoneda().isEmpty()) {
            try {
                t.setMoneda(Tienda.Moneda.valueOf(request.getMoneda()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Moneda no válida: " + request.getMoneda());
            }
        }

        t.setDescripcion(request.getDescripcion());
        t.setDireccion(request.getDireccion());
        t.setHorarios(request.getHorarios());
        t.setMapa_url(request.getMapa_url());

        // === MANEJO DE LA IMAGEN DEL LOGO CON CLOUDINARY ===
        if (request.getLogoImg() != null && !request.getLogoImg().isEmpty()) {
            try {
                // Opciones: subir a una carpeta organizada
                Map<String, Object> options = Map.of(
                        "folder", "tiendas/logos",
                        "overwrite", true,
                        "resource_type", "image"
                );

                Map uploadResult = cloudinaryService.upload(request.getLogoImg(), options);
                String secureUrl = (String) uploadResult.get("secure_url");

                // Si estamos editando y había una imagen anterior → borrarla de Cloudinary
                if (id != null && t.getLogo_img_url() != null && !t.getLogo_img_url().isEmpty()) {
                    String oldPublicId = extractPublicId(t.getLogo_img_url());
                    if (oldPublicId != null) {
                        try {
                            cloudinaryService.delete(oldPublicId);
                        } catch (Exception e) {
                            // No rompemos el flujo si falla el borrado (puede ser imagen ya borrada)
                            System.out.println("No se pudo borrar la imagen anterior: " + e.getMessage());
                        }
                    }
                }

                t.setLogo_img_url(secureUrl);

            } catch (IOException e) {
                throw new RuntimeException("Error al subir el logo a Cloudinary: " + e.getMessage());
            }
        }
        // Si no se envía nueva imagen → mantiene la actual (o null en creación)

        // === ACTIVO (solo ADMIN) ===
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin && request.getActivo() != null) {
            t.setActivo(request.getActivo());
        }

        // === PLAN ===
        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            t.setPlan(plan);
        } else if (id != null) {
            // Permite quitar el plan en edición
            t.setPlan(null);
        }

        // Guardar y devolver respuesta
        return toResponse(tiendaRepository.save(t));
    }

    /**
     * Extrae el public_id de una URL de Cloudinary
     * Ej: https://res.cloudinary.com/dqznlmig0/image/upload/v123/tiendas/logos/mi_logo.jpg
     * → devuelve "tiendas/logos/mi_logo"
     */
    private String extractPublicId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            // Quitar la parte inicial hasta /upload/
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String path = url.substring(uploadIndex + 8); // +8 para saltar "/upload/"

            // Quitar la versión (v1234567890/)
            int versionIndex = path.indexOf("/");
            if (versionIndex != -1) {
                path = path.substring(versionIndex + 1);
            }

            // Quitar la extensión .jpg, .png, etc.
            int dotIndex = path.lastIndexOf(".");
            if (dotIndex != -1) {
                path = path.substring(0, dotIndex);
            }

            return path;
        } catch (Exception e) {
            return null;
        }
    }
    private String extractPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        // Extrae "carpeta/imagen" (sin extensión y sin versión)
        String[] parts = url.split("/");
        String fileWithExt = parts[parts.length - 1];
        String fileWithoutExt = fileWithExt.substring(0, fileWithExt.lastIndexOf('.'));

        // Si hay carpeta: "carpeta/" + fileWithoutExt
        String publicId = fileWithoutExt;
        if (parts.length > 6) {  // Si hay carpeta
            publicId = parts[parts.length - 2] + "/" + fileWithoutExt;
        }

        return publicId;
    }
    @Override
    @Transactional(readOnly = true)
    public TiendaResponse getTiendaByIdParaEdicion(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // Verificar que el usuario es dueño o es ADMIN
        if (!tienda.getUser().getEmail().equals(emailActual) &&
                !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("No tienes permisos para acceder a esta tienda");
        }

        return toResponse(tienda);
    }
    @Override
    @Transactional(readOnly = true)
    public List<DropTownStandar> getTiendasForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay usuario autenticado → lista vacía (seguridad)
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Collections.emptyList();
        }

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Tienda> tiendas;

        if (esAdmin) {
            // ADMIN: ve todas las tiendas (ordenadas por nombre)
            tiendas = tiendaRepository.findAllByOrderByNombreAsc();
        } else {
            // OWNER o cualquier otro rol autenticado: solo su propia tienda
            Tienda tiendaActual = getTiendaDelUsuarioActual();
            if (tiendaActual == null) {
                return Collections.emptyList();
            }
            tiendas = Collections.singletonList(tiendaActual);
        }

        // Mapeo a DTO estándar
        return tiendas.stream()
                .map(t -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(t.getId());
                    dto.setDescripcion(t.getNombre());
                    return dto;
                })
                .toList();
    }

    // Método auxiliar para verificar permisos
    private boolean tienePermisoParaTienda(Integer tiendaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return tiendaRepository.findById(tiendaId)
                .map(tienda -> esAdmin || tienda.getUser().getEmail().equals(emailActual))
                .orElse(false);
    }


    @Override
    public void deleteById(Integer id) {
        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // Solo el dueño puede eliminarla
        if (!tienda.getUser().getEmail().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new RuntimeException("No puedes eliminar una tienda que no es tuya");
        }

        tiendaRepository.delete(tienda);
    }

    // TiendaServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findByUserEmail(String email, Pageable pageable) {
        return tiendaRepository.findByUserEmail(email, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public TiendaResponse toggleActivo(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verificar si el usuario autenticado es ADMIN
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden activar o desactivar tiendas");
        }

        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada con ID: " + id));

        // Toggle del estado activo
        tienda.setActivo(!tienda.getActivo());

        Tienda saved = tiendaRepository.save(tienda);

        return toResponse(saved); // tu método de mapeo a DTO
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
        if (t.getPlan() != null) {
            dto.setPlanId(t.getPlan().getId());
            dto.setPlanNombre(t.getPlan().getNombre());
        }
        dto.setUserId(t.getUser().getId());
        dto.setUserEmail(t.getUser().getEmail());
        return dto;
    }


}