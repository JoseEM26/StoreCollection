package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.TiendaRequest;
import com.proyecto.StoreCollection.dto.response.TiendaResponse;
import com.proyecto.StoreCollection.entity.Plan;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.PlanRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TiendaServiceImpl implements TiendaService {

    @Autowired
    private TiendaRepository tiendaRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TiendaResponse> findAll(Pageable pageable) {
        return tiendaRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse findById(Long id) {
        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + id));
        return toResponse(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public TiendaResponse findBySlug(String slug) {
        Tienda tienda = tiendaRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada por slug: " + slug));
        return toResponse(tienda);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiendaResponse> findByUserId(Long userId) {
        return tiendaRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TiendaResponse save(TiendaRequest request) { return save(request, null); }

    @Override
    public TiendaResponse save(TiendaRequest request, Long id) {
        Tienda t = id == null ? new Tienda() : tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + id));

        t.setNombre(request.getNombre());
        t.setSlug(request.getSlug());
        t.setWhatsapp(request.getWhatsapp());
        t.setMoneda(Tienda.Moneda.valueOf(request.getMoneda()));
        t.setDescripcion(request.getDescripcion());
        t.setDireccion(request.getDireccion());
        t.setHorarios(request.getHorarios());

        if (request.getPlanId() != null) {
            Plan plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            t.setPlan(plan);
        }

        Usuario user = usuarioRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        t.setUser(user);

        return toResponse(tiendaRepository.save(t));
    }

    @Override
    public void deleteById(Long id) {
        if (!tiendaRepository.existsById(id)) {
            throw new RuntimeException("Tienda no encontrada: " + id);
        }
        tiendaRepository.deleteById(id);
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
        if (t.getPlan() != null) {
            dto.setPlanId(t.getPlan().getId());
            dto.setPlanNombre(t.getPlan().getNombre());
        }
        dto.setUserId(t.getUser().getId());
        dto.setUserEmail(t.getUser().getEmail());
        return dto;
    }
}