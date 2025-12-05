package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TiendaService tiendaService; // ← para asignar automáticamente la tienda

    // === PÚBLICO: para el menú y filtros del catálogo ===
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findByTiendaSlug(String tiendaSlug) {
        // TenantFilter ya puso el tenantId → solo mostramos las categorías de esa tienda
        return categoriaRepository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    // === PRIVADO: panel del dueño ===
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findAllByTenant() {
        return categoriaRepository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    public Page<CategoriaResponse> findByUserEmail(String email, Pageable pageable) {
        // OWNER: usa tenant actual
        return categoriaRepository.findAllByTenant(pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<CategoriaResponse> buscarPorNombreYEmailUsuario(String nombre, String email, Pageable pageable) {
        Integer tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("Tenant no establecido");

        return categoriaRepository.findByNombreContainingIgnoreCaseAndTenantId(nombre.trim(), tenantId, pageable)
                .map(this::toResponse);
    }
    @Override
    public Page<CategoriaResponse> buscarPorNombreContainingIgnoreCase(String nombre, Pageable pageable) {
        return categoriaRepository.findByNombreContainingIgnoreCase(nombre, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaResponse> findAll(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse findById(Integer id) {
        Categoria categoria = categoriaRepository.getByIdAndTenant(id); // ← solo si es suyo
        return toResponse(categoria);
    }

    @Override
    public CategoriaResponse save(CategoriaRequest request) {
        return save(request, null);
    }

    @Override
    public CategoriaResponse save(CategoriaRequest request, Integer id) {
        Categoria cat = id == null
                ? new Categoria()
                : categoriaRepository.getByIdAndTenant(id); // ← seguridad: solo puede editar las suyas

        cat.setNombre(request.getNombre());
        cat.setSlug(request.getSlug());
        cat.setTienda(tiendaService.getTiendaDelUsuarioActual()); // ← automático, 100% seguro

        return toResponse(categoriaRepository.save(cat));
    }

    @Override
    public void deleteById(Integer id) {
        Categoria cat = categoriaRepository.getByIdAndTenant(id);
        categoriaRepository.delete(cat);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(
                c.getId(),
                c.getNombre(),
                c.getSlug(),
                c.getTienda().getId()
        );
    }
}