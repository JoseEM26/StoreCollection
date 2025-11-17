package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VarianteRepository extends JpaRepository<Variante ,Long> {
}
