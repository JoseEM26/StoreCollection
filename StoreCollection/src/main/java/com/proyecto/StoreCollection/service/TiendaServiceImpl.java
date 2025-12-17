// src/main/java/com/proyecto/StoreCollection/service/TiendaServiceImpl.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.dto.special.DashboardTiendaPublicDTO;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.PlanRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findAll(Pageable pageable) {
        return tiendaRepository.findAll(pageable).map(this::toResponse);
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
            // CREACIÓN
            t = new Tienda();

            // Validar que userId sea el usuario actual (o ADMIN creando para otros)
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

            // Si no es ADMIN, solo puede crear para sí mismo
            if (!esAdmin && !usuario.getEmail().equals(emailActual)) {
                throw new RuntimeException("No puedes crear una tienda para otro usuario");
            }

            t.setUser(usuario);
        } else {
            // EDICIÓN
            t = tiendaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

            // Verificar permisos: solo el dueño o ADMIN puede editar
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            Optional<Tienda> tiendaConMismoSlug = tiendaRepository.findBySlug(request.getSlug());
            if (tiendaConMismoSlug.isPresent() && !tiendaConMismoSlug.get().getId().equals(id)) {
                throw new RuntimeException("Ya existe otra tienda con ese slug: " + request.getSlug());
            }
            if (!t.getUser().getEmail().equals(emailActual) && !esAdmin) {
                throw new RuntimeException("No tienes permisos para editar esta tienda");
            }

            // En edición, mantener usuario existente (no cambiar dueño)
            // Solo ADMIN podría cambiar el dueño si se necesita, pero aquí no permitimos
        }

        // Setear campos comunes
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
        t.setLogo_img_url(request.getLogo_img_url());

        // Manejar campo activo (solo ADMIN puede cambiar)
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (esAdmin && request.getActivo() != null) {
            t.setActivo(request.getActivo());
        }

        // Manejar plan
        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            t.setPlan(plan);
        } else if (id != null) {
            // Permitir quitar el plan en edición
            t.setPlan(null);
        }

        return toResponse(tiendaRepository.save(t));
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
    public List<DropDownStandard> getTiendasForDropdown() {
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
                    DropDownStandard dto = new DropDownStandard();
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