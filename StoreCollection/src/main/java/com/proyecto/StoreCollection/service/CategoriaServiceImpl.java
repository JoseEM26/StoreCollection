package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TiendaService tiendaService; // ← para asignar automáticamente la tienda
    private final ProductoRepository productoRepository; // ← para asignar automáticamente la tienda

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findByTiendaSlug(String tiendaSlug) {
        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tienda no disponible");
        }

        return categoriaRepository.findByTiendaIdAndActivoTrue(tenantId).stream()
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
    @Transactional(readOnly = true)
    public List<DropTownStandar> getCategoriasForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Categoria> categorias;

        if (esAdmin) {
            categorias = categoriaRepository.findAllByOrderByNombreAsc();
        } else {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            categorias = categoriaRepository.findByTiendaIdOrderByNombreAsc(tenantId);
        }

        return categorias.stream()
                .filter(Categoria::isActivo)  // Opcional: solo categorías activas
                .map(c -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(c.getId());
                    dto.setDescripcion(c.getNombre());
                    return dto;
                })
                .toList();
    }
    // === NUEVO: Para obtener categoría específica en edición (con verificación de permisos) ===
    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse getCategoriaByIdParaEdicion(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Solo el dueño de la tienda o ADMIN puede acceder para edición
        if (!esAdmin && !categoria.getTienda().getUser().getEmail().equals(emailActual)) {
            throw new AccessDeniedException("No tienes permisos para acceder a esta categoría");
        }

        return toResponse(categoria);
    }
    @Override
    public CategoriaResponse save(CategoriaRequest request) {
        return save(request, null);
    }
    @Override
    @Transactional
    public CategoriaResponse save(CategoriaRequest request, Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Categoria categoria;
        Tienda tiendaAsignada;

        if (id == null) {
            // ==================== CREACIÓN ====================
            categoria = new Categoria();

            if (esAdmin) {
                // ADMIN: debe proporcionar tiendaId obligatoriamente
                if (request.getTiendaId() == null) {
                    throw new RuntimeException("El campo tiendaId es obligatorio para administradores al crear una categoría");
                }
                tiendaAsignada = tiendaService.getEntityById(request.getTiendaId());
            } else {
                // OWNER: asignación automática
                tiendaAsignada = tiendaService.getTiendaDelUsuarioActual();
            }

        } else {
            // ==================== EDICIÓN ====================
            categoria = categoriaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            // Verificación de permisos
            if (!esAdmin && !categoria.getTienda().getUser().getEmail().equals(emailActual)) {
                throw new AccessDeniedException("No tienes permisos para editar esta categoría");
            }

            if (esAdmin && request.getTiendaId() != null) {
                // ADMIN puede cambiar la tienda en edición
                tiendaAsignada = tiendaService.getEntityById(request.getTiendaId());
            } else {
                // OWNER o ADMIN sin cambiar tienda → mantiene la original
                tiendaAsignada = categoria.getTienda();
            }
        }

        // Validación de unicidad del slug DENTRO de la tienda asignada
        categoriaRepository.findBySlugAndTiendaId(request.getSlug().trim(), tiendaAsignada.getId())
                .ifPresent(c -> {
                    if (id == null || !c.getId().equals(id)) {
                        throw new RuntimeException("Ya existe una categoría con el slug '" + request.getSlug() + "' en esta tienda");
                    }
                });

        // Asignar datos
        categoria.setNombre(request.getNombre().trim());
        categoria.setSlug(request.getSlug().trim());
        categoria.setTienda(tiendaAsignada);

        Categoria saved = categoriaRepository.save(categoria);
        return toResponse(saved);
    }
    @Override
    @Transactional
    public CategoriaResponse toggleActivo(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!esAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden activar o desactivar categorías");
        }

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        boolean nuevoEstado = !categoria.isActivo();
        categoria.setActivo(nuevoEstado);

        if (nuevoEstado) {
            // ← NUEVO: Al activar la categoría, reactivar todos sus productos
            productoRepository.activarTodosPorCategoriaId(id);
        } else {
            // Al desactivar la categoría, desactivar todos sus productos
            productoRepository.desactivarTodosPorCategoriaId(id);
        }

        Categoria saved = categoriaRepository.save(categoria);
        return toResponse(saved);
    }
    @Override
    public void deleteById(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailActual = auth.getName();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        // Verificación: solo dueño o ADMIN
        if (!esAdmin && !categoria.getTienda().getUser().getEmail().equals(emailActual)) {
            throw new AccessDeniedException("No tienes permisos para eliminar esta categoría");
        }

        categoriaRepository.delete(categoria);
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(
                c.getId(),
                c.getNombre(),
                c.getSlug(),
                c.isActivo(),
                c.getTienda().getId()
        );
    }
}