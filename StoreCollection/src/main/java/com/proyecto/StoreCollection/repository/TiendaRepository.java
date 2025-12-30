package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {

    Page<Tienda> findByActivoTrue(Pageable pageable);
    List<Tienda> findByActivoTrue();
    List<Tienda> findAllByOrderByNombreAsc();
    Optional<Tienda> findBySlug(String slug);

    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Tienda> findByUserId(Integer userId);

    Page<Tienda> findByUserEmail(String email, Pageable pageable);

    Optional<Tienda> findFirstByUserEmail(String email);

    // ==================== TIENDAS PÚBLICAS ACTIVAS ====================

    @Query("SELECT t FROM Tienda t WHERE t.activo = true AND t.plan.slug IN ('basico', 'pro')")
    Page<Tienda> findAllPublicasActivas(Pageable pageable);

    @Query("SELECT t FROM Tienda t WHERE t.activo = true AND t.plan.slug IN ('basico', 'pro')")
    List<Tienda> findAllPublicasActivas();

    @Query("SELECT t FROM Tienda t WHERE t.activo = true AND t.plan.slug IN :planesPermitidos")
    Page<Tienda> findAllPublicasActivas(@Param("planesPermitidos") Set<String> planesPermitidos, Pageable pageable);

    @Query("SELECT t FROM Tienda t WHERE t.activo = true AND t.plan.slug IN :planesPermitidos")
    List<Tienda> findAllPublicasActivas(@Param("planesPermitidos") Set<String> planesPermitidos);
}