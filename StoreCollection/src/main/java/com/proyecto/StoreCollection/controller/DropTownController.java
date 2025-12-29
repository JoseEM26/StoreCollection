package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.DropTown.AtributoDropdownDTO;
import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class DropTownController {

    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final TiendaService tiendaService;
    private final AtributoService atributoService;
    private final PlanService planService;

    // Lista de Tiendas simplificada (solo activas)
    @GetMapping("/tiendasDropTown")
    public ResponseEntity<List<DropTownStandar>> getTiendasDropdown() {
        List<DropTownStandar> lista = tiendaService.getTiendasForDropdown();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/planesDropTown")
    public ResponseEntity<List<DropTownStandar>> getPlanesDropdown() {
        List<DropTownStandar> lista = planService.findDropdownPlanesActivos();
        return ResponseEntity.ok(lista);
    }

    // Lista de Usuarios simplificada
    @GetMapping("/usuariosDropTown")
    public ResponseEntity<List<DropTownStandar>> getUsuariosDropdown() {
        List<DropTownStandar> lista = usuarioService.getUsuariosForDropdown();
        return ResponseEntity.ok(lista);
    }
    @GetMapping("/categoriasDropTown")
    public ResponseEntity<List<DropTownStandar>> getCategoriasDropdown() {
        List<DropTownStandar> lista = categoriaService.getCategoriasForDropdown();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/atributosDropTown")
    public ResponseEntity<List<AtributoDropdownDTO>> getAtributosDropdown() {
        List<AtributoDropdownDTO> lista = atributoService.getAtributosConValores();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/productoDropTown")
    public ResponseEntity<List<DropTownStandar>> getProductosDropdown() {
        List<DropTownStandar> lista = productoService.getProductosForDropdown();
        return ResponseEntity.ok(lista);
    }
}