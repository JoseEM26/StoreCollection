// CategoriaRepository.java → Versión FINAL 100% FUNCIONAL

package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer>, TenantBaseRepository<Categoria, Integer> {

    Page<Categoria> findAll(Pageable pageable);
    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<Categoria> findBySlugAndTiendaId(String slug, Integer tiendaId);
    @Query("SELECT c FROM Categoria c WHERE c.tienda.id = :tenantId AND LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Categoria> findByNombreContainingIgnoreCaseAndTenantId(
            @Param("nombre") String nombre,
            @Param("tenantId") Integer tenantId,
            Pageable pageable);

    default Page<Categoria> findAllByTenant(Pageable pageable) {
        Integer tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("Tenant no establecido");
        return findAllByTiendaId(tenantId, pageable);
    }

    @Query("SELECT c FROM Categoria c WHERE c.tienda.id = :tenantId")
    Page<Categoria> findAllByTiendaId(@Param("tenantId") Integer tenantId, Pageable pageable);
}