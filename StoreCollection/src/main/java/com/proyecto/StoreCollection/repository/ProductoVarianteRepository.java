// src/main/java/com/proyecto/StoreCollection/repository/ProductoVarianteRepository.java

package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.ProductoVariante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoVarianteRepository
        extends TenantBaseRepository<ProductoVariante, Integer> {

     default List<ProductoVariante> findByProductoIdSafe(Integer productoId) {
          return findAllByTenant().stream()
                  .filter(v -> v.getProducto().getId().equals(productoId))
                  .toList();
     }

     @Query("SELECT v FROM ProductoVariante v WHERE v.producto.slug = :productoSlug AND v.tienda.slug = :tiendaSlug")
     List<ProductoVariante> findByTiendaSlugAndProductoSlug(
             @Param("tiendaSlug") String tiendaSlug,
             @Param("productoSlug") String productoSlug);
}