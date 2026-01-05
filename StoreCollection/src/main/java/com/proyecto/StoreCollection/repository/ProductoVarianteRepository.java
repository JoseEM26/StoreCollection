package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.ProductoVariante;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoVarianteRepository extends TenantBaseRepository<ProductoVariante, Integer> {

     // ==================== CONTADORES ====================

     /**
      * Cuenta cuántas variantes tiene una tienda específica.
      * Usado para validar el límite max_variantes del plan.
      */
     //long countByTiendaId(Integer tiendaId);
     int countByTiendaId(Integer tiendaId);
     /**
      * Cuenta cuántas variantes tiene un producto específico.
      * Muy útil al editar un producto: restamos sus variantes actuales del conteo total
      * para no penalizar al usuario por variantes que ya existían.
      */
     long countByProductoId(Integer productoId);

     // ==================== OPERACIONES MASIVAS POR PRODUCTO ====================

     @Modifying
     @Query("UPDATE ProductoVariante pv SET pv.activo = false WHERE pv.producto.id = :productoId")
     void desactivarTodasPorProductoId(@Param("productoId") Integer productoId);

     @Modifying
     @Query("UPDATE ProductoVariante pv SET pv.activo = true WHERE pv.producto.id = :productoId")
     void activarTodasPorProductoId(@Param("productoId") Integer productoId);

     // ==================== BÚSQUEDA PÚBLICA ====================

     @Query("SELECT v FROM ProductoVariante v " +
             "WHERE v.producto.slug = :productoSlug " +
             "AND v.tienda.slug = :tiendaSlug")
     List<ProductoVariante> findByTiendaSlugAndProductoSlug(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);

     // ==================== MÉTODO SEGURO CON TENANT ====================

     /**
      * Filtra variantes por producto usando el tenant actual.
      * Evita exposición accidental de datos de otras tiendas.
      */
     default List<ProductoVariante> findByProductoIdSafe(Integer productoId) {
          return findAllByTenant().stream()
                  .filter(v -> v.getProducto().getId().equals(productoId))
                  .toList();
     }
}