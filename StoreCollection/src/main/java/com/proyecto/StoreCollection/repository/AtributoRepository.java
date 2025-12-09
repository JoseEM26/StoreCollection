package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.dto.special.AtributoConValores;
import com.proyecto.StoreCollection.entity.Atributo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtributoRepository extends TenantBaseRepository<Atributo, Integer> {
    // Para ADMIN: todos los atributos con sus valores cargados
    @EntityGraph(attributePaths = "valores")
    @Query("SELECT a FROM Atributo a ORDER BY a.nombre ASC")
    List<Atributo> findAllWithValoresOrderByNombre();

    // Para OWNER o ADMIN filtrando por tienda específica
    @EntityGraph(attributePaths = "valores")
    @Query("SELECT a FROM Atributo a WHERE a.tienda.id = :tiendaId ORDER BY a.nombre ASC")
    List<Atributo> findByTiendaIdWithValoresOrderByNombre(@Param("tiendaId") Integer tiendaId);
    }