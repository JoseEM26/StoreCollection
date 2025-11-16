package com.proyecto.StoreCollection.Repository;

import com.proyecto.StoreCollection.entity.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "variantes", path = "variantes")
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {
}