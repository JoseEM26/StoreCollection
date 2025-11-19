package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AtributoServiceImpl implements AtributoService {

    private final AtributoRepository repository;
    private final TiendaService tiendaService; // ← clave para obtener la tienda del usuario

    // === PÚBLICO: para filtros en catálogo ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findByTiendaSlug(String tiendaSlug) {
        // El TenantFilter ya puso el tenantId → solo usamos findAllByTenant()
        return repository.findAllByTenant().stream()
                .map(this::toResponse)
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
    public AtributoResponse findById(Long id) {
        Atributo atributo = repository.getByIdAndTenant(id); // ← solo si es suyo
        return toResponse(atributo);
    }

    @Override
    public AtributoResponse save(AtributoRequest request) {
        return save(request, null);
    }

    @Override
    public AtributoResponse save(AtributoRequest request, Long id) {
        Atributo atributo = id == null
                ? new Atributo()
                : repository.getByIdAndTenant(id); // ← solo puede editar los suyos

        atributo.setNombre(request.getNombre());
        atributo.setTienda(tiendaService.getTiendaDelUsuarioActual()); // ← automático

        return toResponse(repository.save(atributo));
    }

    @Override
    public void deleteById(Long id) {
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