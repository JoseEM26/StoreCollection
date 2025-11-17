package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito,Long> {

     List<Carrito> findBySessionId(String sessionId) ;
     void deleteBySessionId(String sessionId) ;
}
