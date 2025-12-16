package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE Usuario u SET u.password = :password WHERE u.id = :id")
    void updatePasswordById(@Param("id") Integer id, @Param("password") String password);
}