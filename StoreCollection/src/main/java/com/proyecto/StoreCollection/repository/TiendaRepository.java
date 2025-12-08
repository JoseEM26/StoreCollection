package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.dto.special.TiendaDropdown;
import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {

    Optional<Tienda> findBySlug(String slug);  // Usa Optional
    Page<Tienda> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    List<Tienda> findByUserId(Integer userId);
    Page<Tienda> findByUserEmail(String email, Pageable pageable);
    Optional<Tienda> findFirstByUserEmail(String email);

    List<TiendaDropdown> findByActivoTrueOrderByNombreAsc();
    @Query("SELECT new com.proyecto.StoreCollection.dto.special.TiendaDropdown(t.id, t.nombre) " +
            "FROM Tienda t WHERE t.user.email = :email AND t.activo = true ORDER BY t.nombre ASC")
    List<TiendaDropdown> findByUserEmailAndActivoTrueOrderByNombreAsc(@Param("email") String email);}