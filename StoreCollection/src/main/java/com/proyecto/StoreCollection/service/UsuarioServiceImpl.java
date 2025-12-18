package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.UsuarioRequest;
import com.proyecto.StoreCollection.dto.response.UsuarioResponse;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public List<DropDownStandard> getUsuariosForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Usuario> usuarios;

        if (esAdmin) {
            // ADMIN ve todos los usuarios, ordenados por nombre
            usuarios = repository.findAllByOrderByNombreAsc();
        } else {
            // OWNER ve solo su propio usuario (de su tienda actual)
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            Tienda tiendaActual = tiendaService.getTiendaDelUsuarioActual(); // Asume que tienes este método
            usuarios = Collections.singletonList(tiendaActual.getUser());
        }

        // Convertir a DTO
        return usuarios.stream()
                .map(u -> {
                    DropDownStandard dto = new DropDownStandard();
                    dto.setId(u.getId());
                    dto.setDescripcion(StringUtils.defaultIfBlank(u.getNombre(), u.getEmail())); // Usa nombre o email
                    return dto;
                })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
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
        dto.setEmail(u.getEmail());
        dto.setCelular(u.getCelular());
        dto.setRol(u.getRol().name());
        return dto;
    }

    /// ///////////////////////ESTO ES PARA HASHEAR CONTRSAEÑAS

    public List<Usuario> findAllRaw() {
        return repository.findAll();
    }

    public void updatePasswordDirectly(Integer id, String nuevaPasswordHasheada) {
        repository.updatePasswordById(id, nuevaPasswordHasheada);
    }


}