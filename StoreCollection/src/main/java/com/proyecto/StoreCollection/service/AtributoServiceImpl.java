package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.AtributoDropdownDTO;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtributoServiceImpl implements AtributoService {

    private final AtributoRepository repository;
    private final TiendaService tiendaService;

    // === PARA DROPDOWN EN PRODUCTOS (OWNER Y ADMIN) ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoDropdownDTO> getAtributosConValores() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Atributo> atributos;

        if (esAdmin) {
            atributos = repository.findAllWithValores();
        } else {
            Tienda tienda = tiendaService.getTiendaDelUsuarioActual();
            atributos = repository.findByTiendaIdWithValores(tienda.getId());
        }

        return atributos.stream()
                .map(attr -> {
                    AtributoDropdownDTO dto = new AtributoDropdownDTO();
                    dto.setId(attr.getId());
                    dto.setDescripcion(attr.getNombre());

                    List<DropTownStandar> valoresDto = attr.getValores().stream()
                            .map(val -> {
                                DropTownStandar v = new DropTownStandar();
                                v.setId(val.getId());
                                v.setDescripcion(val.getValor());
                                return v;
                            })
                            .sorted(Comparator.comparing(DropTownStandar::getDescripcion))
                            .toList();

                    dto.setValores(valoresDto);
                    return dto;
                })
                .toList();
    }


    // === LISTAR TODOS (para panel admin de atributos) ===
    @Override
    @Transactional(readOnly = true)
    public Page<AtributoResponse> findAll(Pageable pageable, Integer tiendaId) {
        Page<Atributo> page;
        if (tiendaId != null) {
            page = repository.findByTiendaId(tiendaId, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findByTiendaSlug(String tiendaSlug) {
        Tienda tienda = tiendaService.findEntityBySlug(tiendaSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tienda no encontrada con slug: " + tiendaSlug));

        return repository.findByTiendaIdWithValores(tienda.getId()).stream()
                .map(this::toResponse)
                .toList();
    }
    // === LISTAR SOLO DE LA TIENDA DEL OWNER ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findAllByTenant() {
        Tienda tienda = tiendaService.getTiendaDelUsuarioActual();
        return repository.findByTiendaIdWithValores(tienda.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // === BUSCAR POR ID (con seguridad de tenant) ===
    @Override
    @Transactional(readOnly = true)
    public AtributoResponse findById(Integer id) {
        Atributo atributo = repository.getByIdAndTenant(id);
        return toResponse(atributo);
    }

    @Override
    @Transactional
    public AtributoResponse save(AtributoRequest request, Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Atributo atributo;
        Tienda tienda;
        Integer tiendaId;
        String tiendaNombre;

        if (id == null) {
            // ================== CREACIÓN ==================
            atributo = new Atributo();

            if (esAdmin && request.getTiendaId() != null) {
                // Admin puede elegir cualquier tienda al crear
                tienda = tiendaService.getEntityById(request.getTiendaId());
            } else {
                // Owner siempre usa su tienda
                // Admin sin tiendaId también usa la del usuario actual (o puedes forzar error si prefieres)
                tienda = tiendaService.getTiendaDelUsuarioActual();
            }

        } else {
            // ================== EDICIÓN ==================
            // Cargamos el atributo existente (getByIdAndTenant valida que owner solo edite los suyos)
            atributo = repository.getByIdAndTenant(id);

            // Inicializamos para evitar LazyInitializationException
            Hibernate.initialize(atributo.getTienda());

            // LA TIENDA NUNCA CAMBIA EN EDICIÓN → usamos siempre la original
            tienda = atributo.getTienda();

            // OPCIONAL: validación extra para admin que intente cambiarla
            if (esAdmin && request.getTiendaId() != null
                    && !request.getTiendaId().equals(tienda.getId())) {
                throw new RuntimeException("No se permite cambiar la tienda de un atributo existente.");
            }
        }

        // Capturamos datos para respuesta (mientras sesión abierta)
        tiendaId = tienda.getId();
        tiendaNombre = tienda.getNombre();

        // Validación de unicidad dentro de la misma tienda
        if (repository.findByNombreAndTiendaId(request.getNombre().trim(), tienda.getId())
                .filter(a -> !a.getId().equals(id))
                .isPresent()) {
            throw new RuntimeException("Ya existe un atributo con el nombre '" + request.getNombre().trim() + "' en esta tienda.");
        }

        // Solo actualizamos el nombre (la tienda ya está asignada y no cambia)
        atributo.setNombre(request.getNombre().trim());
        atributo.setTienda(tienda); // Inofensivo, pero claro

        Atributo saved = repository.save(atributo);

        return new AtributoResponse(
                saved.getId(),
                saved.getNombre(),
                tiendaId,
                tiendaNombre,
                null
        );
    }

    @Override
    public void deleteById(Integer id) {
        Atributo atributo = repository.getByIdAndTenant(id);
        repository.delete(atributo);
    }

    private AtributoResponse toResponse(Atributo a) {
        return new AtributoResponse(
                a.getId(),
                a.getNombre(),
                a.getTienda().getId(),
                a.getTienda().getNombre(),
                null
        );
    }
}