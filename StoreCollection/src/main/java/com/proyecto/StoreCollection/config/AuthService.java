package com.proyecto.StoreCollection.config;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio utilitario para obtener información del usuario autenticado.
 * Se usa en cualquier capa (service, controller, filter) sin depender de TenantContext.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * @return true si el usuario actual tiene el rol ROLE_ADMIN
     */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * @return true si el usuario actual tiene el rol ROLE_OWNER (o ADMIN)
     */
    public boolean isOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * @return Email del usuario autenticado (subject del JWT)
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    /**
     * @return true si hay un usuario autenticado (no anónimo)
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }
}