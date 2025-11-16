package com.proyecto.StoreCollection.Repository;

import com.proyecto.StoreCollection.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "planes", path = "planes")
public interface PlanRepository extends JpaRepository<Plan, Long> {
}