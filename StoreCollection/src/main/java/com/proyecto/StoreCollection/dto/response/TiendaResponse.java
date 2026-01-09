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
    private Integer planId;
    private String userEmail;
    private boolean activo;
    // En TiendaResponse.java
    private String planSlug;
    private String estadoSuscripcion;  // trial, active, canceled, etc.
    private String trialEndsAt;
    private String fechaVencimiento;
    private Integer maxProductos;
    private Integer maxVariantes;
    private String emailRemitente;
    private String emailAppPassword;

    private String tiktok;
    private String instagram;
    private String facebook;

}