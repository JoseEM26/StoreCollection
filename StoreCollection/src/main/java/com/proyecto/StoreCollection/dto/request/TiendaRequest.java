package com.proyecto.StoreCollection.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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
    private String emailRemitente;
    private String emailAppPassword;
    private String mapa_url;
    private MultipartFile logoImg;
    private Integer  userId;
    private Integer  planId;
    private Boolean activo;

    private String tiktok;
    private String instagram;
    private String facebook;
}