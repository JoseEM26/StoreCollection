package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoRequest;
import com.proyecto.StoreCollection.dto.response.AtributoResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.repository.TiendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AtributoServiceImpl implements AtributoService {

    @Autowired
    private AtributoRepository repository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    public AtributoResponse save(AtributoRequest request) {
        return save(request, null);
    }

    @Override
    public AtributoResponse save(AtributoRequest request, Long id) {
        Atributo atributo = id == null ? new Atributo() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado: " + id));

        atributo.setNombre(request.getNombre());
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada: " + request.getTiendaId()));
        atributo.setTienda(tienda);

        atributo = repository.save(atributo);
        return toResponse(atributo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AtributoResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AtributoResponse findById(Long id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado: " + id));
    }

    @Override
    public void deleteById(Long id) {
        if (!repository.existsById(id)) throw new RuntimeException("Atributo no encontrado: " + id);
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponse> findByTiendaId(Long tiendaId) {
        return repository.findByTiendaId(tiendaId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private AtributoResponse toResponse(Atributo a) {
        AtributoResponse dto = new AtributoResponse();
        dto.setId(a.getId());
        dto.setNombre(a.getNombre());
        dto.setTiendaId(a.getTienda().getId());
        return dto;
    }
}