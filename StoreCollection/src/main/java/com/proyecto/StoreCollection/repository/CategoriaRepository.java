// CategoriaRepository.java → Versión FINAL 100% FUNCIONAL

package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.dto.special.CategoriaDropdown;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Tienda;
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
    @Query("SELECT c FROM Categoria c WHERE c.id = :id AND c.tienda = :tienda")
    Optional<Categoria> findByIdAndTienda(@Param("id") Integer id, @Param("tienda") Tienda tienda);
    @Query("SELECT c FROM Categoria c WHERE c.tienda.id = :tenantId AND LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    Page<Categoria> findByNombreContainingIgnoreCaseAndTenantId(
            @Param("nombre") String nombre,
            @Param("tenantId") Integer tenantId,
            Pageable pageable);
    List<CategoriaDropdown> findByTiendaIdOrderByNombreAsc(Integer tiendaId);
    default Page<Categoria> findAllByTenant(Pageable pageable) {
        Integer tenantId = com.proyecto.StoreCollection.tenant.TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("Tenant no establecido");
        return findAllByTiendaId(tenantId, pageable);
    }

    @Query("SELECT c FROM Categoria c WHERE c.tienda.id = :tenantId")
    Page<Categoria> findAllByTiendaId(@Param("tenantId") Integer tenantId, Pageable pageable);
}