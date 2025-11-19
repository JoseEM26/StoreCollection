package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.AtributoValor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtributoValorRepository extends TenantBaseRepository<AtributoValor, Long> {

    default List<AtributoValor> findByAtributoIdSafe(Long atributoId) {
        return findAllByTenant().stream()
                .filter(v -> v.getAtributo().getId().equals(atributoId))
                .toList();
    }

    @Query("SELECT v FROM AtributoValor v WHERE v.atributo.id = :atributoId AND v.tienda.id = :tenantId")
    List<AtributoValor> findByAtributoIdAndTenant(
            @Param("atributoId") Long atributoId,
            @Param("tenantId") Long tenantId);
}