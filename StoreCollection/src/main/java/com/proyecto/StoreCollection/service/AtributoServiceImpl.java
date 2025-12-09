package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.dto.special.AtributoConValores;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AtributoServiceImpl implements AtributoService {

    private final AtributoRepository repository;
    private final TiendaService tiendaService;
    private final UsuarioService usuarioService;  // ← Añadido (necesario para esAdmin())
    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role ->
                        "ADMIN".equals(role) || "ROLE_ADMIN".equals(role)
                );
    }
    // === PÚBLICO: filtros en catálogo (cualquiera puede ver atributos de una tienda pública) ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findByTiendaSlug(String tiendaSlug) {
        return repository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    // === PRIVADO: solo dueño logueado (sus atributos) ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findAllByTenant() {
        return repository.findAllByTenant().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AtributoResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AtributoResponse findById(Integer id) {
        Atributo atributo = repository.getByIdAndTenant(id);
        return toResponse(atributo);
    }

    // === Para dropdowns: ADMIN ve todos, OWNER solo los suyos ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoConValores> findAllWithValoresForDropdown() {
        List<Atributo> atributos;

        if (esAdmin()) {
            // ADMIN: todos los atributos del sistema (con valores cargados)
            atributos = repository.findAllWithValoresOrderByNombre();
        } else {
            // OWNER: solo los de su tienda. Si no tiene tienda → vacío (seguro)
            atributos = repository.findAllByTenant().stream()
                    .sorted(Comparator.comparing(Atributo::getNombre))
                    .toList();
        }

        return atributos.stream()
                .map(this::toAtributoConValores)
                .toList();
    }

    // === ADMIN o OWNER (si coincide la tienda) puede usar este método ===
    @Override
    @Transactional(readOnly = true)
    public List<AtributoConValores> findByTiendaIdWithValores(Integer tiendaId) {
        return repository.findByTiendaIdWithValoresOrderByNombre(tiendaId).stream()
                .map(this::toAtributoConValores)
                .toList();
    }

    // === Mapeo reutilizable: Atributo → DTO con valores ordenados ===
    private AtributoConValores toAtributoConValores(Atributo attr) {
        var valoresOrdenados = attr.getValores().stream()
                .sorted(Comparator.comparing(
                        v -> v.getValor() != null ? v.getValor().toLowerCase() : ""
                ))
                .map(v -> new AtributoConValores.ValorDto(v.getId(), v.getValor()))
                .toList();

        return new AtributoConValores(attr.getId(), attr.getNombre(), valoresOrdenados);
    }

    // === CREAR ===
    @Override
    public AtributoResponse save(AtributoRequest request) {
        return save(request, null);
    }

    // === ACTUALIZAR ===
    @Override
    public AtributoResponse save(AtributoRequest request, Integer id) {
        Atributo atributo = id == null
                ? new Atributo()
                : repository.getByIdAndTenant(id); // solo puede editar los suyos

        atributo.setNombre(request.getNombre());
        atributo.setTienda(tiendaService.getTiendaDelUsuarioActual());

        return toResponse(repository.save(atributo));
    }

    // === ELIMINAR ===
    @Override
    public void deleteById(Integer id) {
        Atributo atributo = repository.getByIdAndTenant(id);
        repository.delete(atributo);
    }

    // === Mapeo básico ===
    private AtributoResponse toResponse(Atributo a) {
        return new AtributoResponse(a.getId(), a.getNombre(), a.getTienda().getId());
    }
}