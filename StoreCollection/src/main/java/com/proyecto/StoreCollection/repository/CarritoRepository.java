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


     // Opcional: contar items
     long countBySessionIdAndTiendaId(String sessionId, Integer tiendaId);
     List<Carrito> findBySessionIdAndTiendaId(String sessionId, Integer tiendaId);
     void deleteBySessionIdAndTiendaId(String sessionId, Integer tiendaId);
     // Y si usas findBySessionIdWithDetails, crea uno nuevo con tiendaId o úsalo con JOIN
     @Query("SELECT c FROM Carrito c JOIN FETCH c.variante v JOIN FETCH v.producto WHERE c.sessionId = :sessionId AND c.tienda.id = :tiendaId")
     List<Carrito> findBySessionIdAndTiendaIdWithDetails(@Param("sessionId") String sessionId, @Param("tiendaId") Integer tiendaId);
}
