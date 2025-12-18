package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.AtributoValor;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.repository.AtributoValorRepository;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AtributoValorServiceImpl implements AtributoValorService {

    private final AtributoValorRepository valorRepository;
    private final AtributoRepository atributoRepository;
    private final TiendaService tiendaService;

    // === IMPLEMENTACIÓN FALTANTE (OBLIGATORIA) ===
    @Override
    @Transactional(readOnly = true)
    public Page<AtributoValorResponse> findAll(Pageable pageable) {
        return valorRepository.findAll(pageable).map(this::toResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public List<AtributoValorResponse> findByAtributoId(Integer atributoId) {
        atributoRepository.getByIdAndTenant(atributoId); // seguridad
        return valorRepository.findByAtributoIdSafe(atributoId).stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<DropDownStandard> getValoresForDropdown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<AtributoValor> valores;

        if (esAdmin) {
            valores = valorRepository.findAllByOrderByValorAsc();
        } else {
            Integer tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Collections.emptyList();
            }
            valores = valorRepository.findByTiendaIdOrderByValorAsc(tenantId);
        }

        return valores.stream()
                .map(v -> {
                    DropDownStandard dto = new DropDownStandard();
                    dto.setId(v.getId());
                    dto.setDescripcion(v.getValor());
                    return dto;
                })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<AtributoValorResponse> findByAtributoIdAndTiendaSlug(
            Integer atributoId, String tiendaSlug) {

        // TenantFilter ya garantiza que solo ve valores de esa tienda
        return valorRepository.findAllByTenant().stream()
                .filter(v -> v.getAtributo().getId().equals(atributoId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AtributoValorResponse findById(Integer id) {
        return toResponse(valorRepository.getByIdAndTenant(id));
    }

    @Override
    public AtributoValorResponse save(AtributoValorRequest request) {
        return save(request, null);
    }

    @Override
    public AtributoValorResponse save(AtributoValorRequest request, Integer id) {
        AtributoValor valor = id == null
                ? new AtributoValor()
                : valorRepository.getByIdAndTenant(id);

        valor.setValor(request.getValor());

        Atributo atributo = atributoRepository.getByIdAndTenant(request.getAtributoId());
        valor.setAtributo(atributo);

        return toResponse(valorRepository.save(valor));
    }

    @Override
    public void deleteById(Integer id) {
        valorRepository.delete(valorRepository.getByIdAndTenant(id));
    }

    private AtributoValorResponse toResponse(AtributoValor v) {
        return new AtributoValorResponse(
                v.getId(),
                v.getValor(),
                v.getAtributo().getId(),
                v.getAtributo().getNombre()
        );
    }
}