package com.proyecto.StoreCollection.dto.response;

import lombok.Data;

@Data
public class CarritoResponse {
    private Integer id;
    private String sessionId;
    private Integer cantidad;
    private Integer varianteId;

    // Datos para mostrar en el carrito
    private String nombreProducto;
    private String sku;
    private Double precio;        // Usamos Double en DTO para fácil manejo en JSON/Frontend
    private String imagenUrl;
    private String atributos;     // Ej: "Rojo, Talla M"
}