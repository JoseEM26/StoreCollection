package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {

    Optional<Tienda> findBySlug(String slug);  // Usa Optional
    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    List<Tienda> findByUserId(Integer userId);
    Optional<Tienda> findByUserEmail(String email);

    Optional<Tienda> findFirstByUserEmail(String email);

}