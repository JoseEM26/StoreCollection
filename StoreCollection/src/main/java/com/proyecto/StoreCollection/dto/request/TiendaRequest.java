package com.proyecto.StoreCollection.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TiendaRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String slug;
    private String whatsapp;
    private String moneda;
    private String descripcion;
    private String direccion;
    private String horarios;
    private Long planId;
    private Long userId;
}