package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuarioRequest {
    @NotBlank
    private String nombre;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private String celular;
    private String rol;
}