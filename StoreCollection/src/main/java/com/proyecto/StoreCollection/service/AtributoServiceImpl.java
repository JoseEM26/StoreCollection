package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.AtributoDropdownDTO;
import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AtributoServiceImpl implements AtributoService {

    private final AtributoRepository repository;
    private final TiendaService tiendaService; // ← clave para obtener la tienda del usuario
    private final AtributoRepository atributoRepository; // ← clave para obtener la tienda del usuario

    // === PÚBLICO: para filtros en catálogo ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findByTiendaSlug(String tiendaSlug) {
        // El TenantFilter ya puso el tenantId → solo usamos findAllByTenant()
        return repository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }
// En tu clase AtributoServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public List<AtributoDropdownDTO> getAtributosConValores() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Atributo> atributos;

        if (esAdmin) {
            atributos = atributoRepository.findAllWithValores();  // ← Usa el método con FETCH
        } else {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            atributos = atributoRepository.findByTiendaIdWithValores(tenantId);
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
    // === PRIVADO: dueño logueado ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findAllByTenant() {
        return repository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AtributoResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AtributoResponse findById(Integer id) {
        Atributo atributo = repository.getByIdAndTenant(id); // ← solo si es suyo
        return toResponse(atributo);
    }

    @Override
    public AtributoResponse save(AtributoRequest request) {
        return save(request, null);
    }

    @Override
    public AtributoResponse save(AtributoRequest request, Integer id) {
        Atributo atributo = id == null
                ? new Atributo()
                : repository.getByIdAndTenant(id); // ← solo puede editar los suyos

        atributo.setNombre(request.getNombre());
        atributo.setTienda(tiendaService.getTiendaDelUsuarioActual()); // ← automático

        return toResponse(repository.save(atributo));
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
                a.getTienda().getId()
        );
    }
}