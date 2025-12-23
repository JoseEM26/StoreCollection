package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.UsuarioRequest;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.dto.response.UsuarioResponse;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final TiendaService tiendaService; // Para obtener la tienda del OWNER
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponse save(UsuarioRequest request) {
        return save(request, null);
    }

    @Override
    public UsuarioResponse save(UsuarioRequest request, Integer id) {
        Usuario usuario;

        if (id == null) {
            usuario = new Usuario();
        } else {
            usuario = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        }

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setCelular(request.getCelular());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRol() != null && !request.getRol().isBlank()) {
            usuario.setRol(Usuario.Rol.valueOf(request.getRol()));
        }

        usuario = repository.save(usuario);
        return toResponse(usuario);
    }
    @Override
    @Transactional(readOnly = true)
    public List<DropTownStandar> getUsuariosForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Usuario> usuarios;

        if (esAdmin) {
            usuarios = repository.findAllByOrderByNombreAsc();
        } else {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            Tienda tiendaActual = tiendaService.getTiendaDelUsuarioActual();
            if (tiendaActual == null || tiendaActual.getUser() == null) {
                return Collections.emptyList();
            }
            usuarios = Collections.singletonList(tiendaActual.getUser());
        }

        return usuarios.stream()
                .map(u -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(u.getId());

                    // Prioriza nombre, si está vacío usa email
                    String nombre = u.getNombre();
                    String descripcion = (nombre != null && !nombre.trim().isEmpty())
                            ? nombre
                            : u.getEmail();

                    dto.setDescripcion(descripcion);
                    return dto;
                })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> findAll(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Usuario> paginaEntity;

        if (StringUtils.hasText(search)) {
            search = "%" + search.trim().toLowerCase() + "%";
            paginaEntity = repository.findBySearchTerm(search, pageable);
        } else {
            paginaEntity = repository.findAll(pageable);
        }

        Page<UsuarioResponse> paginaDto = paginaEntity.map(this::toResponse);

        return new PageResponse<>(
                paginaDto.getContent(),
                paginaDto.getNumber(),
                paginaDto.getSize(),
                paginaDto.getTotalElements(),
                paginaDto.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioResponse> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toResponse);
    }

    @Override
    public void deleteById(Integer id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private UsuarioResponse toResponse(Usuario u) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setActivo(u.isActivo());
        dto.setEmail(u.getEmail());
        dto.setCelular(u.getCelular());
        dto.setRol(u.getRol().name());
        return dto;
    }
    @Override
    public UsuarioResponse toggleActivarUsuario(Integer id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        // Toggle: cambiar el estado actual
        usuario.setActivo(!usuario.isActivo());

        usuario = repository.save(usuario);

        return toResponse(usuario);
    }
    /// ///////////////////////ESTO ES PARA HASHEAR CONTRSAEÑAS

    public List<Usuario> findAllRaw() {
        return repository.findAll();
    }

    public void updatePasswordDirectly(Integer id, String nuevaPasswordHasheada) {
        repository.updatePasswordById(id, nuevaPasswordHasheada);
    }


}