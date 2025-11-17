package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
     List<Categoria> findByTiendaId(Long tiendaId);
}