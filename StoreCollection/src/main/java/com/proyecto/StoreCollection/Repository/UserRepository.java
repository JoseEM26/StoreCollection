package com.proyecto.StoreCollection.Repository;

import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "usuarios", path = "usuarios")
public interface UserRepository extends JpaRepository<Usuario, Long> {
}