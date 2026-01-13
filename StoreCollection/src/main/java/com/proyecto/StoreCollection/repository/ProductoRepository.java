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

     // ==================== CONTADORES ====================

     /**
      * Cuenta cuántos productos tiene una tienda específica.
      * Muy útil para validar límites del plan (max_productos).
      */
     //long countByTiendaId(Integer tiendaId);

     // ==================== BÚSQUEDAS BÁSICAS ====================
     Integer countByTiendaId(Integer tiendaId);
     Optional<Producto> findBySlugAndTiendaId(String slug, Integer tiendaId);
     Integer countByTiendaIdAndActivoTrue(Integer tiendaId);
     List<Producto> findAllByOrderByNombreAsc();

     List<Producto> findByTiendaIdOrderByNombreAsc(Integer tiendaId);

     @Query("SELECT p FROM Producto p WHERE p.tienda.id = :tenantId")
     Page<Producto> findAllByTiendaId(@Param("tenantId") Integer tenantId, Pageable pageable);

     // Búsqueda global (ADMIN)
     Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

     // Búsqueda por nombre en tienda específica (OWNER)
     @Query("SELECT p FROM Producto p " +
             "WHERE p.tienda.id = :tenantId " +
             "AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
     Page<Producto> findByNombreContainingIgnoreCaseAndTenantId(
             @Param("nombre") String nombre,
             @Param("tenantId") Integer tenantId,
             Pageable pageable);

     // ==================== POR CATEGORÍA ====================

     @Query("SELECT p FROM Producto p " +
             "WHERE p.categoria.id = :categoriaId AND p.tienda.id = :tenantId")
     List<Producto> findByCategoriaIdAndTiendaId(
             @Param("categoriaId") Integer categoriaId,
             @Param("tenantId") Integer tenantId);

     default List<Producto> findByCategoriaIdSafe(Integer categoriaId) {
          Integer tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
          if (tenantId == null) {
               throw new IllegalStateException("Tenant no establecido");
          }
          return findByCategoriaIdAndTiendaId(categoriaId, tenantId);
     }

     // ==================== PÚBLICO ====================

     @Query("SELECT p FROM Producto p " +
             "WHERE p.slug = :productoSlug AND p.tienda.slug = :tiendaSlug")
     Optional<Producto> findByTiendaSlugAndProductoSlug(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);

     @Query("SELECT DISTINCT p FROM Producto p " +
             "LEFT JOIN FETCH p.categoria " +
             "LEFT JOIN FETCH p.variantes v " +
             "WHERE p.tienda.slug = :tiendaSlug")
     List<Producto> findByTiendaSlugPublic(@Param("tiendaSlug") String tiendaSlug);

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
        pv.id                  AS variante_id,
        p.id                   AS producto_id,
        p.nombre               AS producto_nombre,
        p.slug                 AS producto_slug,
        c.nombre               AS categoria_nombre,
        pv.precio              AS precio,
        pv.stock               AS stock,
        pv.imagen_url          AS imagen_url,
        pv.activo              AS variante_activo,
        p.activo               AS producto_activo,
        pv.precio_anterior     AS precio_anterior,
        pv.descripcion_corta   AS descripcion_corta,
        a.nombre               AS atributo_nombre,
        av.valor               AS atributo_valor
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

     // ==================== OPERACIONES MASIVAS ====================
     @Query("""
    SELECT p FROM Producto p
    LEFT JOIN FETCH p.variantes v
    WHERE p.id = :id
      AND p.tienda.id = :tiendaId
""")
     Optional<Producto> findByIdAndTiendaIdWithVariantes(
             @Param("id") Integer id,
             @Param("tiendaId") Integer tiendaId);
     @Modifying
     @Query("UPDATE Producto p SET p.activo = false WHERE p.categoria.id = :categoriaId")
     void desactivarTodosPorCategoriaId(@Param("categoriaId") Integer categoriaId);

     @Modifying
     @Query("UPDATE Producto p SET p.activo = true WHERE p.categoria.id = :categoriaId")
     void activarTodosPorCategoriaId(@Param("categoriaId") Integer categoriaId);
}