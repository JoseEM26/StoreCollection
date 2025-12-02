package com.proyecto.StoreCollection.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductoCardResponse {

    private Integer id;
    private String nombre;
    private String slug;
    private String nombreCategoria;

    private BigDecimal precioMinimo = BigDecimal.ZERO;
    private Integer stockTotal = 0;
    private String imagenPrincipal;

    private List<VarianteCard> variantes = new ArrayList<>();

    // ==================== GETTERS & SETTERS ====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public BigDecimal getPrecioMinimo() { return precioMinimo; }
    public void setPrecioMinimo(BigDecimal precioMinimo) { this.precioMinimo = precioMinimo; }

    public Integer getStockTotal() { return stockTotal; }
    public void setStockTotal(Integer stockTotal) { this.stockTotal = stockTotal; }

    public String getImagenPrincipal() { return imagenPrincipal; }
    public void setImagenPrincipal(String imagenPrincipal) { this.imagenPrincipal = imagenPrincipal; }

    public List<VarianteCard> getVariantes() { return variantes; }
    public void setVariantes(List<VarianteCard> variantes) { this.variantes = variantes; }

    // ==================== CLASE INTERNA VARIANTE ====================

    public static class VarianteCard {
        private BigDecimal precio;
        private Integer stock;
        private String imagenUrl;
        private boolean activo = true;

        public BigDecimal getPrecio() { return precio; }
        public void setPrecio(BigDecimal precio) { this.precio = precio; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }

        public String getImagenUrl() { return imagenUrl; }
        public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }
    }
}