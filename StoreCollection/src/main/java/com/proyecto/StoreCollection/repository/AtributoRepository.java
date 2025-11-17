package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Atributo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtributoRepository extends JpaRepository<Atributo,Long> {
    List<Atributo> findByTiendaId(Long tiendaId);
}
