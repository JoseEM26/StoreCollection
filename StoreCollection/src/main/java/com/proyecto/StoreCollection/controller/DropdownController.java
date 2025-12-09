package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.special.CategoriaDropdown;
import com.proyecto.StoreCollection.dto.special.AtributoConValores;
import com.proyecto.StoreCollection.dto.special.TiendaDropdown;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.CategoriaService;
import com.proyecto.StoreCollection.service.TiendaService;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DropdownController {

    private final CategoriaService categoriaService;
    private final AtributoService atributoService;
    private final TiendaService tiendaService;

    // 1. Categorías para el formulario de producto
    @GetMapping("/api/owner/dropdown/categorias")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<List<CategoriaDropdown>> dropdownCategorias() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<CategoriaDropdown> resultado;

        if (esAdmin) {
            resultado = categoriaService.findAllForDropdown();
        } else {
            Integer tenantId = TenantContext.getTenantId();
            resultado = (tenantId != null)
                    ? categoriaService.findByTiendaIdForDropdown(tenantId)
                    : List.of();
        }

        return ResponseEntity.ok(resultado);
    }

    // 2. Atributos + valores (Color, Talla, etc.)
    @GetMapping("/api/owner/dropdown/atributos")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<List<AtributoConValores>> dropdownAtributos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<AtributoConValores> resultado;

        if (esAdmin) {
            resultado = atributoService.findAllWithValoresForDropdown();
        } else {
            Integer tenantId = TenantContext.getTenantId();
            resultado = (tenantId != null)
                    ? atributoService.findByTiendaIdWithValores(tenantId)
                    : List.of();
        }

        return ResponseEntity.ok(resultado);
    }

    // 3. Tiendas (solo ADMIN)
    @GetMapping("/api/owner/tiendas/dropdown")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TiendaDropdown>> dropdownTiendas() {
        return ResponseEntity.ok(tiendaService.findAllDopTownList());
    }
}