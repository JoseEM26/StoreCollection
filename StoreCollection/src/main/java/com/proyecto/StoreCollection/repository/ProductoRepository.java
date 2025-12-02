// src/main/java/com/proyecto/StoreCollection/repository/ProductoRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Producto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends TenantBaseRepository<Producto, Integer> {

     // Ya no necesitas estos → los borras o los dejas por compatibilidad
     // List<Producto> findByTiendaId(Long tiendaId);
     // List<Producto> findByCategoriaId(Long categoriaId);

     // Para catálogo público: por slug de tienda + slug de producto
     @Query("SELECT p FROM Producto p WHERE p.slug = :productoSlug AND p.tienda.slug = :tiendaSlug")
     Optional<Producto> findByTiendaSlugAndProductoSlug(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);
// src/main/java/com/proyecto/StoreCollection/repository/ProductoRepository.java
@Query("SELECT DISTINCT p FROM Producto p " +
        "LEFT JOIN FETCH p.categoria " +
        "LEFT JOIN FETCH p.variantes v " +
        "WHERE p.tienda.slug = :tiendaSlug")
List<Producto> findByTiendaSlugPublic(@Param("tiendaSlug") String tiendaSlug);


     // Por categoría (seguro con tenant)
     @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.tienda.id = :tenantId")
     List<Producto> findByCategoriaIdAndTiendaId(
             @Param("categoriaId") Integer categoriaId,
             @Param("tenantId") Integer tenantId);

     // Versión segura automática
     default List<Producto> findByCategoriaIdSafe(Integer categoriaId) {
          Integer tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
          if (tenantId == null) throw new IllegalStateException("Tenant no establecido");
          return findByCategoriaIdAndTiendaId(categoriaId, tenantId);
     }



     // ProductoRepository.java

     @Query("""
    SELECT 
        p.id,
        p.nombre,
        p.slug,
        c.nombre,
        pv.precio,
        pv.stock,
        pv.imagenUrl,
        pv.activo
    FROM Producto p
    JOIN p.categoria c
    LEFT JOIN p.variantes pv WITH pv.activo = true
    WHERE p.tienda.slug = :tiendaSlug
      AND p.tienda.activo = true
    ORDER BY p.id, pv.id
    """)
     List<Object[]> findRawCatalogByTiendaSlug(@Param("tiendaSlug") String tiendaSlug);


     @Query("""
    SELECT 
        p.id,
        p.nombre,
        p.slug,
        c.nombre,
        pv.precio,
        pv.stock,
        pv.imagenUrl,
        pv.activo
    FROM Producto p
    JOIN p.categoria c
    LEFT JOIN p.variantes pv WITH pv.activo = true
    WHERE p.tienda.slug = :tiendaSlug 
      AND p.slug = :productoSlug
      AND p.tienda.activo = true
    ORDER BY p.id, pv.id
    """)
     List<Object[]> findRawDetailBySlugs(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);
}