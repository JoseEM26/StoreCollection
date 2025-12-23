// src/main/java/com/proyecto/StoreCollection/repository/ProductoRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends TenantBaseRepository<Producto, Integer> {

     Optional<Producto> findBySlugAndTiendaId(String slug, Integer tiendaId);
     List<Producto> findAllByOrderByNombreAsc();
     List<Producto> findByTiendaIdOrderByNombreAsc(Integer tiendaId);
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
     @Query("SELECT p FROM Producto p WHERE p.tienda.id = :tenantId")
     Page<Producto> findAllByTenantId(@Param("tenantId") Integer tenantId, Pageable pageable);
     // ADMIN: búsqueda global
     Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
     @Modifying
     @Query("UPDATE Producto p SET p.activo = false WHERE p.categoria.id = :categoriaId")
     void desactivarTodosPorCategoriaId(@Param("categoriaId") Integer categoriaId);
     // OWNER: búsqueda solo en su tienda
     @Query("SELECT p FROM Producto p WHERE p.tienda.id = :tenantId AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
     Page<Producto> findByNombreContainingIgnoreCaseAndTenantId(
             @Param("nombre") String nombre,
             @Param("tenantId") Integer tenantId,
             Pageable pageable);
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



     @Query("""
    SELECT 
        p.id,
        p.nombre,
        p.slug,
        c.nombre,
        pv.precio,
        pv.stock,
        pv.imagenUrl,
        pv.activo,
        p.activo
    FROM Producto p
    JOIN p.categoria c
    LEFT JOIN p.variantes pv WITH pv.activo = true
    WHERE p.tienda.slug = :tiendaSlug
      AND p.tienda.activo = true
      AND p.activo = true               
      AND c.activo = true               
    ORDER BY p.id, pv.id
""")
     List<Object[]> findRawCatalogByTiendaSlug(@Param("tiendaSlug") String tiendaSlug);


     @Query(value = """
    SELECT 
        pv.id,                  
        p.id,                   
        p.nombre,               
        p.slug,                 
        c.nombre,              
        pv.precio,              
        pv.stock,               
        pv.imagen_url AS imagenUrl,          
        pv.activo,              
        p.activo,
        a.nombre AS atributo_nombre,
        av.valor
    FROM producto p
    JOIN categoria c ON c.id = p.categoria_id
    LEFT JOIN producto_variante pv ON pv.producto_id = p.id AND pv.activo = true
    LEFT JOIN variante_atributo va ON va.variante_id = pv.id
    LEFT JOIN atributo_valor av ON av.id = va.atributo_valor_id
    LEFT JOIN atributo a ON a.id = av.atributo_id
    WHERE p.tienda_id = (SELECT t.id FROM tienda t WHERE t.slug = :tiendaSlug AND t.activo = true)
      AND p.slug = :productoSlug
      AND p.activo = true              
      AND c.activo = true               
    ORDER BY p.id, pv.id, a.nombre, av.valor
    """, nativeQuery = true)
     List<Object[]> findRawDetailBySlugs(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);



     @Modifying
     @Query("UPDATE Producto p SET p.activo = true WHERE p.categoria.id = :categoriaId")
     void activarTodosPorCategoriaId(@Param("categoriaId") Integer categoriaId);
}