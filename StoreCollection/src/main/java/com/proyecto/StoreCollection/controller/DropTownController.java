package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.repository.AtributoRepository;
import com.proyecto.StoreCollection.repository.UsuarioRepository;
import com.proyecto.StoreCollection.service.AtributoService;
import com.proyecto.StoreCollection.service.TiendaService;
import com.proyecto.StoreCollection.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class DropTownController {

    private final UsuarioRepository usuarioService;
    private final TiendaService tiendaService;
    private final AtributoRepository atributoService;

    // Lista de Tiendas simplificada (solo activas)
    @GetMapping("/tiendasDropTown")
    public ResponseEntity<List<DropTownStandar>> getTiendas() {
        List<Tienda> tiendas = tiendaService.findAllActivas(); // o findAll() si quieres todas

        List<DropTownStandar> dtos = tiendas.stream()
                .map(t -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(t.getId());
                    dto.setDescripcion(t.getDescripcion() != null ? t.getDescripcion() : "");
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Lista de Usuarios simplificada
    @GetMapping("/usuariosDropTown")
    public ResponseEntity<List<DropTownStandar>> getUsuarios() {
        List<Usuario> usuarios = usuarioService.findAll();

        List<DropTownStandar> dtos = usuarios.stream()
                .map(u -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(u.getId());
                    dto.setDescripcion(u.getNombre()); // Usamos nombre como descripción
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Lista de Atributos simplificada
    @GetMapping("/atributosDropTown")
    public ResponseEntity<List<DropTownStandar>> getAtributos() {
        List<Atributo> atributos = atributoService.findAll();

        List<DropTownStandar> dtos = atributos.stream()
                .map(a -> {
                    DropTownStandar dto = new DropTownStandar();
                    dto.setId(a.getId());
                    dto.setDescripcion(a.getNombre()); // Usamos nombre como descripción
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}