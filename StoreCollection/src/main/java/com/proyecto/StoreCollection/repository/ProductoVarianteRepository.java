package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {
     List<ProductoVariante> findByProductoId(Long productoId) ;
}