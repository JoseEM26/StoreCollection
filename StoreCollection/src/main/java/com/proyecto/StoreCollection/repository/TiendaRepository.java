package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {

    List<Tienda> findAllByOrderByNombreAsc();
    Optional<Tienda> findBySlug(String slug);

    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Tienda> findByUserId(Integer userId);
    List<Tienda> findByUserEmail(String email);
    Page<Tienda> findByUserEmail(String email, Pageable pageable);

    Optional<Tienda> findFirstByUserEmail(String email);
    @Query("SELECT t FROM Tienda t LEFT JOIN FETCH t.plan WHERE t.user.email = :email")
    List<Tienda> findByUserEmailWithPlan(@Param("email") String email);

    @Query("SELECT t FROM Tienda t LEFT JOIN FETCH t.plan WHERE t.activo = true")
    Page<Tienda> findByActivoTrueWithPlan(Pageable pageable);

    @Query("SELECT t FROM Tienda t WHERE t.activo = true ")
    Page<Tienda> findAllPublicasActivas(Pageable pageable);


}