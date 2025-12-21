package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class UsuarioResponse {
    private Integer id;
    private String nombre;
    private String email;
    private Boolean activo;
    private String celular;
    private String rol;
}