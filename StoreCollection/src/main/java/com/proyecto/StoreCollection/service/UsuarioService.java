package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.UsuarioRequest;
import com.proyecto.StoreCollection.dto.response.UsuarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UsuarioService {
    Page<UsuarioResponse> findAll(Pageable pageable);
    UsuarioResponse findById(Long id);
    Optional<UsuarioResponse> findByEmail(String email);
    UsuarioResponse save(UsuarioRequest request);
    UsuarioResponse save(UsuarioRequest request, Long id);
    void deleteById(Long id);
}