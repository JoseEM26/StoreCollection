package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.BoletaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletaDetalleRepository extends JpaRepository<BoletaDetalle, Integer> {
}