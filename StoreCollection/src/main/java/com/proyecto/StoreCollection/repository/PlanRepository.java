package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    // Planes activos y ordenados
    List<Plan> findByActivoTrueOrderByOrdenAsc();

    // Planes públicos visibles (página de precios)
    List<Plan> findByActivoTrueAndEsVisiblePublicoTrueOrderByOrdenAsc();
    List<Plan> findByActivoTrue();  // ← AÑADE ESTO
    // Para paginación en admin
    //Page<Plan> findByActivoTrue(Pageable pageable);
    Page<Plan> findByActivoTrueAndEsVisiblePublicoTrue(Pageable pageable);
    // Plan por defecto (más barato)
    @Query("SELECT p FROM Plan p WHERE p.activo = true AND p.esVisiblePublico = true ORDER BY p.precioMensual ASC")
    Optional<Plan> findFirstByActivoTrueAndEsVisiblePublicoTrueOrderByPrecioMensualAsc();

    // Planes para upgrade (más caros que X)
    @Query("SELECT p FROM Plan p WHERE p.activo = true AND p.esVisiblePublico = true AND p.precioMensual > :precioActual ORDER BY p.precioMensual ASC")
    List<Plan> findByActivoTrueAndEsVisiblePublicoTrueAndPrecioMensualGreaterThan(
            @Param("precioActual") BigDecimal precioActual,
            Sort sort
    );

    // Validaciones de slug
    Optional<Plan> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Integer id);
}