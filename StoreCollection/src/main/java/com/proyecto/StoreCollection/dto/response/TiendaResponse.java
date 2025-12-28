package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class  TiendaResponse {
    private Integer id;
    private String nombre;
    private String slug;
    private String whatsapp;
    private String moneda;
    private String descripcion;
    private String direccion;
    private String horarios;
    private String mapa_url;
    private String logo_img_url;
    private String planNombre;
    private Integer userId;
    private String userEmail;
    private boolean activo;
    // En TiendaResponse.java
    private String planSlug;
    private String estadoSuscripcion;  // trial, active, canceled, etc.
    private String trialEndsAt;
    private String fechaFin;
    private Integer maxProductos;
    private Integer maxVariantes;

// + getters y setters para todos
}