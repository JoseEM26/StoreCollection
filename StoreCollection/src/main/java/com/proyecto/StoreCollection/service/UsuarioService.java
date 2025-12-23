package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.request.UsuarioRequest;
import com.proyecto.StoreCollection.dto.response.PageResponse;
import com.proyecto.StoreCollection.dto.response.UsuarioResponse;
import com.proyecto.StoreCollection.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    PageResponse<UsuarioResponse> findAll(int page, int size, String search);
    UsuarioResponse findById(Integer id);
    Optional<UsuarioResponse> findByEmail(String email);
    UsuarioResponse save(UsuarioRequest request);
    UsuarioResponse save(UsuarioRequest request, Integer id);
    UsuarioResponse toggleActivarUsuario(Integer id);
    void deleteById(Integer id);
     List<Usuario> findAllRaw() ;
     void updatePasswordDirectly(Integer id, String nuevaPasswordHasheada) ;
    List<DropTownStandar> getUsuariosForDropdown();
}