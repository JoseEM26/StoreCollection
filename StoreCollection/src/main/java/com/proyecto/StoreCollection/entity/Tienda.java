package com.proyecto.StoreCollection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tienda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "plan"})
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre de la tienda es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El slug es obligatorio")
    @Column(unique = true, nullable = false, length = 120)
    private String slug;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 255)
    private String descripcion;

    @Column(length = 255)
    private String direccion;

    @Column(length = 255)
    private String horarios;

    @Column(name = "mapa_url")
    private String mapaUrl;

    @Column(name = "logo_img_url")
    private String logoImgUrl;

    @Column(name = "tiktok")
    private String tiktok;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "facebook")
    private String facebook;

    @Column(name = "ruc")
    private String ruc;

    @Column(nullable = false)
    private Boolean activo = true;

    // ==================== ENUM MONEDA ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false, length = 20)
    private Moneda moneda = Moneda.SOLES;

    public enum Moneda {
        SOLES, DOLARES
    }

    // ==================== RELACIONES ====================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // ==================== AUDITORÍA ====================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;


}