package com.proyecto.StoreCollection.Repository;

import com.proyecto.StoreCollection.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "categorias", path = "categorias")
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}