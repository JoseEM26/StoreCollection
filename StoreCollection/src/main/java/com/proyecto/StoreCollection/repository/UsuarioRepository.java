package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findAllByOrderByNombreAsc();
    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombre) LIKE :search OR " +
            "LOWER(u.email) LIKE :search OR " +
            "LOWER(u.celular) LIKE :search")
    Page<Usuario> findBySearchTerm(@Param("search") String search, Pageable pageable);
    @Modifying
    @Query("UPDATE Usuario u SET u.password = :password WHERE u.id = :id")
    void updatePasswordById(@Param("id") Integer id, @Param("password") String password);
}