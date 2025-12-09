package com.proyecto.StoreCollection.dto.special;

import java.util.List;

public record AtributoConValores(
        Integer id,
        String nombre,
        List<ValorDto> valores
) {
    public record ValorDto(Integer id, String valor) {}
}