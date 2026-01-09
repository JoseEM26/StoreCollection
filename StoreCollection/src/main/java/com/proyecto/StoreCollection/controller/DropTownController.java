package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.dto.DropTown.AtributoDropdownDTO;
import com.proyecto.StoreCollection.dto.DropTown.DropTownStandar;
import com.proyecto.StoreCollection.dto.response.AtributoValorResponse;
import com.proyecto.StoreCollection.dto.response.VarianteResponse;
import com.proyecto.StoreCollection.dto.special.ProductoAdminListDTO;
import com.proyecto.StoreCollection.entity.Atributo;
import com.proyecto.StoreCollection.entity.Producto;
import com.proyecto.StoreCollection.entity.ProductoVariante;
import com.proyecto.StoreCollection.repository.ProductoRepository;
import com.proyecto.StoreCollection.service.*;
import com.proyecto.StoreCollection.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
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
    private final ProductoRepository productoRepository;
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

    @GetMapping("/productos-con-stock")
    public ResponseEntity<Page<ProductoAdminListDTO>> getProductosConStockParaVenta(
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer productoIdConVariantes) {  // ← NUEVO parámetro opcional

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());

        Integer tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        Page<ProductoAdminListDTO> resultado;

        if (nombre != null && !nombre.trim().isEmpty()) {
            resultado = productoService.buscarPorNombreYTiendaAdminList(tenantId, nombre.trim(), pageable);
        } else {
            resultado = productoService.listarPorTiendaAdminList(tenantId, pageable);
        }

        // Filtrar productos activos y con stock
        List<ProductoAdminListDTO> contenido = resultado.getContent().stream()
                .filter(p -> p.isActivo() && p.getStockTotal() > 0)
                .toList();

        if (productoIdConVariantes != null) {
            contenido = contenido.stream().map(dto -> {
                if (dto.getId().equals(productoIdConVariantes) && dto.isTieneVariantes()) {

                    // ← Cambio importante aquí
                    Producto producto = productoRepository
                            .findByIdAndTiendaIdWithVariantes(productoIdConVariantes, tenantId)
                            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                    List<VarianteResponse> variantes = producto.getVariantes().stream()
                            .filter(ProductoVariante::isActivo)
                            .filter(v -> v.getStock() > 0)
                            .map(this::toVarianteResponseSimple)
                            .toList();

                    dto.setVariantes(variantes);
                }
                return dto;
            }).toList();
        }

        Page<ProductoAdminListDTO> paginaFinal = new PageImpl<>(contenido, pageable, contenido.size());

        return ResponseEntity.ok(paginaFinal);
    }
    private VarianteResponse toVarianteResponseSimple(ProductoVariante variante) {
        VarianteResponse vr = new VarianteResponse();
        vr.setId(variante.getId());
        vr.setSku(variante.getSku() != null ? variante.getSku().trim() : "");
        vr.setPrecio(variante.getPrecio());
        vr.setStock(variante.getStock());
        vr.setImagenUrl(variante.getImagenUrl());
        vr.setActivo(variante.isActivo());

        // Atributos: nombre del atributo + valor (ej: "Talla: M", "Color: Rojo")
        List<AtributoValorResponse> attrs = variante.getAtributos().stream()
                .map(av -> {
                    AtributoValorResponse avr = new AtributoValorResponse();
                    avr.setId(av.getId());
                    avr.setAtributoNombre(av.getAtributo().getNombre());
                    avr.setValor(av.getValor());
                    return avr;
                })
                .sorted(Comparator.comparing(AtributoValorResponse::getAtributoNombre))
                .toList();

        vr.setAtributos(attrs);
        return vr;
    }
}