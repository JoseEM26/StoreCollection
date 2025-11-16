package com.proyecto.StoreCollection.DTO;


public record RegisterRequest(
        String nombre,
        String email,
        String password,
        String celular
) {}