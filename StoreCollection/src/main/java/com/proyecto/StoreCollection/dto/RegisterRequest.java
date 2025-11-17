package com.proyecto.StoreCollection.dto;


public record RegisterRequest(
        String nombre,
        String email,
        String password,
        String celular
) {}