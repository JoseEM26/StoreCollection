package com.proyecto.StoreCollection.Repository;

import com.proyecto.StoreCollection.entity.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "tiendas", path = "tiendas")
public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    // Solo el dueño o admin puede ver sus tiendas
    @RestResource(exported = false)
    Tienda findByUserId(Long userId);
}