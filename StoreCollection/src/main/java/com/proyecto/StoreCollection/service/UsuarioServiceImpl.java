package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.UsuarioRequest;
import com.proyecto.StoreCollection.dto.response.UsuarioResponse;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UsuarioResponse save(UsuarioRequest request) {
        return save(request, null);
    }

    @Override
    public UsuarioResponse save(UsuarioRequest request, Long id) {
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
    public Page<UsuarioResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
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
    public void deleteById(Long id) {
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
}