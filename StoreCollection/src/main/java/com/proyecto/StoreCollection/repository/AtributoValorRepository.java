package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.entity.AtributoValor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AtributoValorRepository extends TenantBaseRepository<AtributoValor, Integer> {

    default List<AtributoValor> findByAtributoIdSafe(Integer atributoId) {
        return findAllByTenant().stream()
                .filter(v -> v.getAtributo().getId().equals(atributoId))
                .toList();
    }

    @Query("SELECT av FROM AtributoValor av WHERE av.id IN :ids AND av.tienda.id = :tiendaId")
    List<AtributoValor> findAllByIdInAndTiendaId(
            @Param("ids") Set<Integer> ids,
            @Param("tiendaId") Integer tiendaId);
    @Query("SELECT v FROM AtributoValor v WHERE v.atributo.id = :atributoId AND v.tienda.id = :tenantId")
    List<AtributoValor> findByAtributoIdAndTenant(
            @Param("atributoId") Long atributoId,
            @Param("tenantId") Long tenantId);
}