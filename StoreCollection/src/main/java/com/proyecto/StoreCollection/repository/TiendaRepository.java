// src/main/java/com/proyecto/StoreCollection/repository/TiendaRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {
    Page<Tienda> findByActivoTrue(Pageable pageable) ;
    List<Tienda> findAllByOrderByNombreAsc();
    Optional<Tienda> findBySlug(String slug);

    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Tienda> findByUserId(Integer userId);

    Page<Tienda> findByUserEmail(String email, Pageable pageable);

    Optional<Tienda> findFirstByUserEmail(String email);  // ← Este es el que usamos ahora
}