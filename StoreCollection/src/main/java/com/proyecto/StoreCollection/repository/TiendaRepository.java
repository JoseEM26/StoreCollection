// src/main/java/com/proyecto/StoreCollection/repository/TiendaRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {
    Page<Tienda> findByActivoTrue(Pageable pageable) ;
    List<Tienda> findByActivoTrue();
    List<Tienda> findAllByOrderByNombreAsc();
    Optional<Tienda> findBySlug(String slug);

    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Tienda> findByUserId(Integer userId);

    Page<Tienda> findByUserEmail(String email, Pageable pageable);

    Optional<Tienda> findFirstByUserEmail(String email);  // ← Este es el que usamos ahora


    @Query("""
        SELECT t FROM Tienda t
        WHERE t.activo = true
        AND EXISTS (
            SELECT 1 FROM TiendaSuscripcion ts
            WHERE ts.tienda = t
            AND ts.estado IN :estadosValidos
            AND (ts.fechaFin IS NULL OR ts.fechaFin > :ahora)
        )
        """)
    Page<Tienda> findTiendasConSuscripcionVigente(
            @Param("ahora") LocalDateTime ahora,
            @Param("estadosValidos") Set<String> estadosValidos,
            Pageable pageable
    );
  
    @Query("""
        SELECT t FROM Tienda t
        WHERE t.activo = true
        AND EXISTS (
            SELECT 1 FROM TiendaSuscripcion ts
            WHERE ts.tienda = t
            AND ts.estado IN :estadosValidos
            AND (ts.fechaFin IS NULL OR ts.fechaFin > :ahora)
        )
        """)
    List<Tienda> findAllConSuscripcionVigente(
            @Param("ahora") LocalDateTime ahora,
            @Param("estadosValidos") Set<String> estadosValidos
    );
}