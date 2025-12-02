// src/app/pages/publico/catalogo/catalogo.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, combineLatest } from 'rxjs';

import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { Producto, Categoria } from '../../../model';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { CategoriaPublicService } from '../../../service/categoria-public.service';
import { TiendaService } from '../../../service/tienda.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ProductoCardComponent],
  templateUrl: './catalogo.component.html',
  styleUrls: ['./catalogo.component.css']
})
export class CatalogoComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  // Datos
  productos: Producto[] = [];
  todasLasCategorias: Categoria[] = [];
  categoriaActual: Categoria | null = null;

  // Filtros
  busqueda = '';
  precioMax = 5000;
  soloEnStock = false;

  // Estado
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private productoService: ProductoPublicService,
    private categoriaService: CategoriaPublicService,
    private tiendaService: TiendaService
  ) {}

  ngOnInit(): void {
    // Combinar cambios de ruta (slug de categoría) + filtros
    combineLatest([
      this.route.paramMap,
      this.categoriaService.getAll()
    ])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params, categorias]) => {
        this.todasLasCategorias = categorias;
        const categoriaSlug = params.get('categoriaSlug');

        if (categoriaSlug) {
          this.categoriaActual = categorias.find(c => c.slug === categoriaSlug) || null;
        } else {
          this.categoriaActual = null;
        }

        this.cargarProductos();
      });
  }

  private cargarProductos(): void {
    this.loading = true;

    this.productoService.getAll().subscribe({
      next: (todos) => {
        let filtrados = [...todos];

        // Filtrar por categoría si aplica
        if (this.categoriaActual) {
          filtrados = filtrados.filter(p => p.categoria?.id === this.categoriaActual!.id);
        }

        this.productos = filtrados;
        this.aplicarFiltros(); // Aplicar búsqueda, precio, stock
        this.loading = false;
      },
      error: () => {
        this.productos = [];
        this.loading = false;
      }
    });
  }

  // === FILTROS ===
  aplicarFiltros() {
    let filtrados = this.productos;

    // Búsqueda
    if (this.busqueda.trim()) {
      const term = this.busqueda.toLowerCase();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term) ||
        p.nombre?.toLowerCase().includes(term)
      );
    }

    // Precio máximo (usamos el menor precio de variantes)
    filtrados = filtrados.filter(p => {
      const precioMin = p.variantes?.length
        ? Math.min(...p.variantes.map(v => v.precio))
        : 0;
      return precioMin <= this.precioMax;
    });

    // Solo en stock
    if (this.soloEnStock) {
      filtrados = filtrados.filter(p => {
        if (!p.variantes?.length) return false;
        return p.variantes.some(v => v.activo && v.stock > 0);
      });
    }

    this.productos = filtrados;
  }

  limpiarFiltros() {
    this.busqueda = '';
    this.precioMax = 5000;
    this.soloEnStock = false;
    this.cargarProductos();
  }

  // Título dinámico
  get tituloPagina(): string {
    return this.categoriaActual?.nombre || 'Todos los Productos';
  }

  get totalResultados(): number {
    return this.productos.length;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}