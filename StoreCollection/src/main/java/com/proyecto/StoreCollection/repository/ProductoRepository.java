package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
     List<Producto> findByTiendaId(Long tiendaId) ;
     List<Producto> findByCategoriaId(Long categoriaId) ;
}