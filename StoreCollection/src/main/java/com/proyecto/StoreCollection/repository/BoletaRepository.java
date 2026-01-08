package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Boleta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Integer> {

    List<Boleta> findBySessionId(String sessionId);
    List<Boleta> findByTiendaId(Integer tiendaId);

    // Métodos con paginación (los que realmente debes usar en producción)
    Page<Boleta> findBySessionId(String sessionId, Pageable pageable);
    Page<Boleta> findByTiendaId(Integer tiendaId, Pageable pageable);
    @Query("SELECT COALESCE(SUM(b.total), 0) FROM Boleta b WHERE b.estado = :estado")
    BigDecimal sumTotalByEstado(@Param("estado") Boleta.EstadoBoleta estado);

    @Query("SELECT COALESCE(SUM(b.total), 0) " +
            "FROM Boleta b " +
            "WHERE b.estado = :estado AND b.tienda.id IN :tiendaIds")
    BigDecimal sumTotalByEstadoAndTiendaIdIn(
            @Param("estado") Boleta.EstadoBoleta estado,
            @Param("tiendaIds") Collection<Integer> tiendaIds
    );
    long countByTiendaIdIn(Collection<Integer> tiendaIds);

    long countByEstadoAndTiendaIdIn(Boleta.EstadoBoleta estado, Collection<Integer> tiendaIds);
    // Si usas más filtros, agrégalos también
    Page<Boleta> findByEstado(Boleta.EstadoBoleta estado, Pageable pageable);
    Page<Boleta> findByTiendaIdAndEstado(Integer tiendaId, Boleta.EstadoBoleta estado, Pageable pageable);
    Page<Boleta> findByTiendaIdAndSessionId(Integer tiendaId, String sessionId, Pageable pageable);
}