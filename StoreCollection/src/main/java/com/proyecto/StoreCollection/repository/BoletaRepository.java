package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Integer> {
    List<Boleta> findBySessionId(String sessionId);

    List<Boleta> findByTiendaId(Integer tiendaId);
}