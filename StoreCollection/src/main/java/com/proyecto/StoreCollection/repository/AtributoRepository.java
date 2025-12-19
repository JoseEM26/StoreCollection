package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Atributo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtributoRepository extends TenantBaseRepository<Atributo, Integer> {

    Optional<Atributo> findByNombreAndTiendaId(String nombre, Integer tiendaId);

    // Métodos simples y válidos (Spring Data los genera automáticamente)
    List<Atributo> findAllByOrderByNombreAsc();

    List<Atributo> findByTiendaIdOrderByNombreAsc(Integer tiendaId);

    // === MÉTODOS CON FETCH JOIN (CORRECTOS) ===
    @Query("SELECT a FROM Atributo a LEFT JOIN FETCH a.valores v WHERE a.tienda.id = :tiendaId ORDER BY a.nombre")
    List<Atributo> findByTiendaIdWithValores(@Param("tiendaId") Integer tiendaId);

    @Query("SELECT a FROM Atributo a LEFT JOIN FETCH a.valores v ORDER BY a.nombre")
    List<Atributo> findAllWithValores();
}