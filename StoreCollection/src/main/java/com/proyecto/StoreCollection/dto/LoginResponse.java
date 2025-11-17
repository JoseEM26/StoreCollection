package com.proyecto.StoreCollection.dto;

import com.proyecto.StoreCollection.entity.Usuario;

public record LoginResponse(String token, String name, Usuario.Rol role) {}
