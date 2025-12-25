package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito,Integer> {

     List<Carrito> findBySessionId(String sessionId) ;
     void deleteBySessionId(String sessionId) ;
     @Query("SELECT c FROM Carrito c " +
             "JOIN FETCH c.variante v " +
             "JOIN FETCH v.producto p " +
             "LEFT JOIN FETCH v.atributos a " +
             "JOIN FETCH v.tienda t " +
             "WHERE c.sessionId = :sessionId")
     List<Carrito> findBySessionIdWithDetails(@Param("sessionId") String sessionId);
}
