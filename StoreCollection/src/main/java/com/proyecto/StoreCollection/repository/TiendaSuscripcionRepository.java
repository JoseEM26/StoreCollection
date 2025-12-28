package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.TiendaSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface TiendaSuscripcionRepository extends JpaRepository<TiendaSuscripcion, Long> {

    @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END " +
            "FROM TiendaSuscripcion ts " +
            "WHERE ts.tienda.id = :tiendaId " +
            "AND ts.estado IN :estados " +
            "AND (ts.fechaFin IS NULL OR ts.fechaFin > :ahora)")
    boolean existsSuscripcionVigente(
            @Param("tiendaId") Integer tiendaId,
            @Param("ahora") LocalDateTime ahora,
            @Param("estados") Set<String> estados);

    @Query("SELECT ts FROM TiendaSuscripcion ts " +
            "WHERE ts.tienda.id = :tiendaId " +
            "AND ts.estado IN :estados " +
            "AND (ts.fechaFin IS NULL OR ts.fechaFin > :ahora) " +
            "ORDER BY ts.fechaInicio DESC")
    Optional<TiendaSuscripcion> findPrimeraSuscripcionVigente(
            @Param("tiendaId") Integer tiendaId,
            @Param("ahora") LocalDateTime ahora,
            @Param("estados") Set<String> estados);

    @Query("SELECT ts FROM TiendaSuscripcion ts " +
            "WHERE ts.tienda.id = :tiendaId " +
            "ORDER BY ts.fechaInicio DESC LIMIT 1")
    Optional<TiendaSuscripcion> findSuscripcionMasReciente(@Param("tiendaId") Integer tiendaId);
}