package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.DropTown.DropDownStandard;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.AtributoValor;
import com.proyecto.StoreCollection.entity.Tienda;
import com.proyecto.StoreCollection.entity.Usuario;
import com.proyecto.StoreCollection.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class DropTownController {

    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final TiendaService tiendaService;
    private final AtributoValorService atributoValorService;

    // Lista de Tiendas simplificada (solo activas)
    @GetMapping("/tiendasDropTown")
    public ResponseEntity<List<DropDownStandard>> getTiendasDropdown() {
        List<DropDownStandard> lista = tiendaService.getTiendasForDropdown();
        return ResponseEntity.ok(lista);
    }

    // Lista de Usuarios simplificada
    @GetMapping("/usuariosDropTown")
    public ResponseEntity<List<DropDownStandard>> getUsuariosDropdown() {
        List<DropDownStandard> lista = usuarioService.getUsuariosForDropdown();
        return ResponseEntity.ok(lista);
    }
    @GetMapping("/categoriasDropTown")
    public ResponseEntity<List<DropDownStandard>> getCategoriasDropdown() {
        List<DropDownStandard> lista = categoriaService.getCategoriasForDropdown();
        return ResponseEntity.ok(lista);
    }

    // Lista de Atributos simplificada
    @GetMapping("/atributosDropTown")
    public ResponseEntity<List<DropDownStandard>> getAtributosDropdown() {
        List<DropDownStandard> valores = atributoValorService.getValoresForDropdown();
        return ResponseEntity.ok(valores);
    }
    @GetMapping("/productoDropTown")
    public ResponseEntity<List<DropDownStandard>> getProductosDropdown() {
        List<DropDownStandard> lista = productoService.getProductosForDropdown();
        return ResponseEntity.ok(lista);
    }
}