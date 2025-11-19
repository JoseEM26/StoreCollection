// src/main/java/com/proyecto/StoreCollection/repository/TenantBaseRepository.java
package com.proyecto.StoreCollection.repository;

import com.proyecto.StoreCollection.tenant.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean // IMPORTANTE: esto evita que Spring cree una instancia de esta interfaz
public interface TenantBaseRepository<T, ID> extends JpaRepository<T, ID> {

    // Filtro automático usando el tenant actual
    default List<T> findAllByTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant no establecido. ¿Pasaste por el TenantFilter?");
        }
        return findAllByTiendaId(tenantId);
    }

    default Optional<T> findByIdAndTenant(ID id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant no establecido");
        }
        return findByIdAndTiendaId(id, tenantId);
    }

    default T getByIdAndTenant(ID id) {
        return findByIdAndTenant(id)
                .orElseThrow(() -> new RuntimeException("Entidad no encontrada o no pertenece al tenant"));
    }

    // Métodos que SÍ puedes sobrescribir con @Query si quieres más eficiencia
    @Query("SELECT e FROM #{#entityName} e WHERE e.tienda.id = :tiendaId")
    List<T> findAllByTiendaId(Long tiendaId);

    @Query("SELECT e FROM #{#entityName} e WHERE e.tienda.id = :tiendaId AND e.id = :id")
    Optional<T> findByIdAndTiendaId(ID id, Long tiendaId);
}