package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.CategoriaRequest;
import com.proyecto.StoreCollection.dto.response.CategoriaResponse;
import com.proyecto.StoreCollection.entity.Categoria;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.repository.CategoriaRepository;
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
public class CategoriaServiceImpl implements CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaResponse> findAll(Pageable pageable) {
        return categoriaRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> findByTiendaId(Long tiendaId) {
        return categoriaRepository.findByTiendaId(tiendaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse findById(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));
        return toResponse(categoria);
    }

    @Override
    public CategoriaResponse save(CategoriaRequest request) { return save(request, null); }

    @Override
    public CategoriaResponse save(CategoriaRequest request, Long id) {
        Categoria cat = id == null ? new Categoria() : categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));

        cat.setNombre(request.getNombre());
        cat.setSlug(request.getSlug());
        Tienda t = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        cat.setTienda(t);

        return toResponse(categoriaRepository.save(cat));
    }

    @Override
    public void deleteById(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada: " + id);
        }
        categoriaRepository.deleteById(id);
    }

    private CategoriaResponse toResponse(Categoria c) {
        CategoriaResponse dto = new CategoriaResponse();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setSlug(c.getSlug());
        dto.setTiendaId(c.getTienda().getId());
        return dto;
    }
}