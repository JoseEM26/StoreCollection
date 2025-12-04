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
    private Integer planId;
    private String planNombre;
    private Integer userId;
    private String userEmail;
    private boolean activo;
}