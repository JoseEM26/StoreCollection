package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.dto.special.CategoriaDropdown;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TiendaService tiendaService; // ← para asignar automáticamente la tienda
    private final TiendaRepository tiendaRepository; // ← para asignar automáticamente la tienda

    // === PÚBLICO: para el menú y filtros del catálogo ===
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findByTiendaSlug(String tiendaSlug) {
        // TenantFilter ya puso el tenantId → solo mostramos las categorías de esa tienda
        return categoriaRepository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDropdown> findAllForDropdown() {
        // Solo ADMIN
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")).stream()
                .map(c -> new CategoriaDropdown(c.getId(), c.getNombre()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaDropdown> findByTiendaIdForDropdown(Integer tiendaId) {
        // OWNER y ADMIN cuando filtra
        return categoriaRepository.findByTiendaIdOrderByNombreAsc(tiendaId).stream()
                .map(c -> new CategoriaDropdown(c.id(), c.nombre()))
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
        Categoria cat = (id == null)
                ? new Categoria()
                : Optional.ofNullable(categoriaRepository.getByIdAndTenant(id))
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada o no tienes permiso"));

        cat.setNombre(request.getNombre().trim());
        cat.setSlug(request.getSlug().trim());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Tienda tienda;

        if (esAdmin && request.getTiendaId() != null && request.getTiendaId() > 0) {
            // Admin quiere asignar a una tienda específica
            tienda = tiendaRepository.findById(request.getTiendaId())
                    .orElseThrow(() -> new RuntimeException("Tienda no encontrada con ID: " + request.getTiendaId()));
        } else {
            // Es OWNER o ADMIN sin tiendaId → usamos la tienda del usuario actual
            tienda = tiendaService.getTiendaDelUsuarioActual();

            // AQUÍ ESTÁ LA CLAVE: validamos que el OWNER tenga tienda
            if (tienda == null) {
                String email = auth.getName(); // o ((User) auth.getPrincipal()).getEmail()
                throw new RuntimeException(
                        "No tienes una tienda asociada a tu cuenta. " +
                                "Por favor, crea tu tienda primero antes de agregar categorías."
                );
            }
        }

        cat.setTienda(tienda);

        Categoria guardada = categoriaRepository.save(cat);
        return toResponse(guardada);
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