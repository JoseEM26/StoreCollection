// src/main/java/com/proyecto/StoreCollection/service/TiendaServiceImpl.java
package com.proyecto.StoreCollection.service;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Tienda t = id == null ? new Tienda() : tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        t.setNombre(request.getNombre());
        t.setSlug(request.getSlug());
        t.setWhatsapp(request.getWhatsapp());
        t.setMoneda(Tienda.Moneda.valueOf(request.getMoneda()));
        t.setDescripcion(request.getDescripcion());
        t.setDireccion(request.getDireccion());
        t.setHorarios(request.getHorarios());
        t.setMapa_url(request.getMapa_url());
        t.setLogo_img_url(request.getLogo_img_url());

        // Solo asignar usuario si es creación (id == null)
        if (id == null) {
            if (request.getUserId() == null) {
                throw new RuntimeException("userId es requerido para crear una tienda");
            }
            t.setUser(usuarioRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado")));
        }
        // En edición, mantener el usuario existente (no tocar)

        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            t.setPlan(plan);
        } else if (id != null) {
            // Opcional: permitir quitar el plan (setear null)
            t.setPlan(null);
        }

        return toResponse(tiendaRepository.save(t));
    }
    public List<Tienda> findAllActivas() {
        return tiendaRepository.findByActivoTrue();
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