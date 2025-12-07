// src/main/java/com/proyecto/StoreCollection/service/TiendaServiceImpl.java
package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.PlanRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;

    // === UTILIDADES ===
    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String getEmailUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Usuario no autenticado");
        }
        return auth.getName();
    }

    // === LISTADOS ===
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
    public Tienda getTiendaDelUsuarioActual() {
        String email = getEmailUsuarioActual();
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
        String email = getEmailUsuarioActual();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return findByUserId(usuario.getId());
    }

    @Override
    public Page<TiendaResponse> buscarPorNombreContainingIgnoreCase(String texto, Pageable pageable) {
        return tiendaRepository.findByNombreContainingIgnoreCase(texto, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<TiendaResponse> findByUserEmail(String email, Pageable pageable) {
        return tiendaRepository.findByUserEmail(email, pageable)
                .map(this::toResponse);
    }

    // === CREAR / ACTUALIZAR (ADMIN PUEDE TODO, OWNER SOLO SUYA) ===
    @Override
    public TiendaResponse save(TiendaRequest request) {
        return save(request, null);
    }

    @Override
    public TiendaResponse save(TiendaRequest request, Integer id) {
        Tienda t = id == null ? new Tienda() : tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // Mapeo de datos
        t.setNombre(request.getNombre());
        t.setSlug(request.getSlug());
        t.setWhatsapp(request.getWhatsapp());
        t.setMoneda(Tienda.Moneda.valueOf(request.getMoneda()));
        t.setDescripcion(request.getDescripcion());
        t.setDireccion(request.getDireccion());
        t.setHorarios(request.getHorarios());
        t.setMapa_url(request.getMapa_url());
        t.setLogo_img_url(request.getLogo_img_url());

        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            t.setPlan(plan);
        }

        // === SEGURIDAD MEJORADA ===
        if (id != null) {
            // EDICIÓN
            if (!esAdmin()) {
                // Si NO es admin → solo puede editar SU tienda
                Tienda actual = getTiendaDelUsuarioActual();
                if (!actual.getId().equals(id)) {
                    throw new RuntimeException("No tienes permiso para editar esta tienda");
                }
            }
            // ADMIN → puede editar cualquier tienda (sin restricción)
        } else {
            // CREACIÓN
            if (!esAdmin()) {
                // Solo el usuario logueado puede crear su propia tienda
                String email = getEmailUsuarioActual();
                Usuario usuario = usuarioRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                t.setUser(usuario);
            } else {
                // ADMIN puede crear tienda y asignarla a quien quiera (opcional)
                // Si quieres que admin asigne userId, descomenta:
                // if (request.getUserId() != null) { t.setUser(...); }
                // Por ahora: el admin crea como si fuera owner
                String email = getEmailUsuarioActual();
                Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
                t.setUser(usuario);
            }
        }

        return toResponse(tiendaRepository.save(t));
    }

    // === ELIMINAR (ADMIN PUEDE TODO, OWNER SOLO SUYA) ===
    @Override
    public void deleteById(Integer id) {
        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        if (!esAdmin()) {
            // Solo el dueño puede eliminar su tienda
            String emailActual = getEmailUsuarioActual();
            if (!tienda.getUser().getEmail().equals(emailActual)) {
                throw new RuntimeException("No tienes permiso para eliminar esta tienda");
            }
        }
        // ADMIN → puede eliminar cualquier tienda

        tiendaRepository.delete(tienda);
    }

    // === MAPPER ===
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