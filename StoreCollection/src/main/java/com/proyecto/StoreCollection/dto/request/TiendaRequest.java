package com.proyecto.StoreCollection.dto.request;

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
    private String mapa_url;
    private MultipartFile logoImg;
    private Integer userId;
    private Boolean activo;
}