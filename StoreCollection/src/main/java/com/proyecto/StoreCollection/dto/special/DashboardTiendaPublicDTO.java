package com.proyecto.StoreCollection.dto.special;

import lombok.Data;

@Data
public class DashboardTiendaPublicDTO {
    //ESTO ES PARA EL DASHBOARD Y PODER LISTAR TODOS LOS PRODUCTOS U MOSTRAR EN PAGINACION en la parte publica
    private Integer id;
    private String nombre;
    private String slug;
    private String descripcion;
    private String direccion;
    private String horarios;
    private Integer planId;
    private String planNombre;
    private String logo_img_url;

}
