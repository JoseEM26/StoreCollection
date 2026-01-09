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
    @Transactional  // ¡Asegúrate de tener esta anotación!
    public AtributoResponse save(AtributoRequest request, Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Atributo atributo;
        Tienda tienda;
        String tiendaNombre;  // ← Guardamos el nombre aquí mientras la sesión está abierta
        Integer tiendaId;

        if (id == null) {
            // === CREACIÓN ===
            atributo = new Atributo();
            if (esAdmin && request.getTiendaId() != null) {
                tienda = tiendaService.getEntityById(request.getTiendaId());
            } else {
                tienda = tiendaService.getTiendaDelUsuarioActual();
            }
            tiendaId = tienda.getId();
            tiendaNombre = tienda.getNombre();  // ← Aquí SÍ está cargada completamente
        } else {
            // === EDICIÓN ===
            atributo = repository.getByIdAndTenant(id);  // Dentro de transacción → sesión abierta

            // Forzamos la inicialización del proxy de tienda mientras la sesión está abierta
            Hibernate.initialize(atributo.getTienda());

            tienda = atributo.getTienda();
            tiendaId = tienda.getId();
            tiendaNombre = tienda.getNombre();  // ← Ahora seguro, porque ya inicializamos
        }

        // Validación unicidad
        if (repository.findByNombreAndTiendaId(request.getNombre().trim(), tienda.getId())
                .filter(a -> !a.getId().equals(id))
                .isPresent()) {
            throw new RuntimeException("Ya existe un atributo con el nombre '" + request.getNombre() + "' en esta tienda.");
        }

        atributo.setNombre(request.getNombre().trim());
        atributo.setTienda(tienda);

        Atributo saved = repository.save(atributo);

        return new AtributoResponse(
                saved.getId(),
                saved.getNombre(),
                tiendaId,
                tiendaNombre,  // ← Usamos las variables capturadas ANTES del posible cierre de sesión
                null
        );
    }
    // === ELIMINAR ===
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