package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class TiendaResponse {
    private Long id;
    private String nombre;
    private String slug;
    private String whatsapp;
    private String moneda;
    private String descripcion;
    private String direccion;
    private String horarios;
    private Long planId;
    private String planNombre;
    private Long userId;
    private String userEmail;
}