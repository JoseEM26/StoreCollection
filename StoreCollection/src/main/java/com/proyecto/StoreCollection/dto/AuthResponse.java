package com.proyecto.StoreCollection.dto;

import com.proyecto.StoreCollection.entity.Usuario;

public record AuthResponse(
        String token,
        Integer id,
        String nombre,
        String email,
        Usuario.Rol rol
) {}