package com.proyecto.StoreCollection.service;

import com.proyecto.StoreCollection.dto.request.AtributoValorRequest;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.AtributoValor;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.repository.AtributoValorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AtributoValorServiceImpl implements AtributoValorService {

    @Autowired
    private AtributoValorRepository repository;

    @Autowired
    private AtributoRepository atributoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AtributoValorResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AtributoValorResponse findById(Long id) {
        AtributoValor valor = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valor no encontrado: " + id));
        return toResponse(valor);
    }
    @Override
    public AtributoValorResponse save(AtributoValorRequest request) { return save(request, null); }

    @Override
    public AtributoValorResponse save(AtributoValorRequest request, Long id) {
        AtributoValor valor = id == null ? new AtributoValor() : repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valor no encontrado: " + id));

        valor.setValor(request.getValor());
        Atributo atributo = atributoRepository.findById(request.getAtributoId())
                .orElseThrow(() -> new RuntimeException("Atributo no encontrado: " + request.getAtributoId()));
        valor.setAtributo(atributo);

        valor = repository.save(valor);
        return toResponse(valor);
    }

    @Override
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Valor no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtributoValorResponse> findByAtributoId(Long atributoId) {
        return repository.findByAtributoId(atributoId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AtributoValorResponse toResponse(AtributoValor v) {
        AtributoValorResponse dto = new AtributoValorResponse();
        dto.setId(v.getId());
        dto.setValor(v.getValor());
        dto.setAtributoId(v.getAtributo().getId());
        return dto;
    }
}