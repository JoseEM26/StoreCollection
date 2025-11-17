package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.AtributoValor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtributoValorRepository extends JpaRepository<AtributoValor,Long> {
    List<AtributoValor> findByAtributoId(Long atributoId);
}
