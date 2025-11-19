// src/main/java/com/proyecto/StoreCollection/repository/ProductoRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Producto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends TenantBaseRepository<Producto, Long> {

     // Ya no necesitas estos → los borras o los dejas por compatibilidad
     // List<Producto> findByTiendaId(Long tiendaId);
     // List<Producto> findByCategoriaId(Long categoriaId);

     // Para catálogo público: por slug de tienda + slug de producto
     @Query("SELECT p FROM Producto p WHERE p.slug = :productoSlug AND p.tienda.slug = :tiendaSlug")
     Optional<Producto> findByTiendaSlugAndProductoSlug(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);

     // Por categoría (seguro con tenant)
     @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.tienda.id = :tenantId")
     List<Producto> findByCategoriaIdAndTiendaId(
             @Param("categoriaId") Long categoriaId,
             @Param("tenantId") Long tenantId);

     // Versión segura automática
     default List<Producto> findByCategoriaIdSafe(Long categoriaId) {
          Long tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
          if (tenantId == null) throw new IllegalStateException("Tenant no establecido");
          return findByCategoriaIdAndTiendaId(categoriaId, tenantId);
     }
}