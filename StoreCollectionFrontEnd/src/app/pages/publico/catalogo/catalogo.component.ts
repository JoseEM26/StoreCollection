// src/app/pages/publico/catalogo/catalogo.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, combineLatest } from 'rxjs';

import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { Categoria } from '../../../model';
import { ProductoPublic } from '../../../model/index.dto';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { CategoriaPublicService } from '../../../service/categoria-public.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ProductoCardComponent],
  templateUrl: './catalogo.component.html',
  styleUrls: ['./catalogo.component.css']
})
export class CatalogoComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  productos: ProductoPublic[] = [];
  todosLosProductos: ProductoPublic[] = []; // para filtros
  categorias: Categoria[] = [];
  categoriaActual: Categoria | null = null;

  // Filtros
  busqueda = '';
  precioMax = 5000;
  soloEnStock = false;

  loading = true;

  constructor(
    private route: ActivatedRoute,
    private productoService: ProductoPublicService,
    private categoriaService: CategoriaPublicService
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.route.paramMap,
      this.categoriaService.getAll(),
      this.productoService.getAll()
    ])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params, categorias, productos]) => {
        this.categorias = categorias;
        this.todosLosProductos = productos;
        this.productos = [...productos];

        const categoriaSlug = params.get('categoriaSlug');
        if (categoriaSlug) {
          this.categoriaActual = categorias.find(c => c.slug === categoriaSlug) || null;
        } else {
          this.categoriaActual = null;
        }

        this.filtrarPorCategoria();
        this.aplicarFiltros();
        this.loading = false;
      });
  }

  private filtrarPorCategoria(): void {
    if (!this.categoriaActual) {
      this.productos = [...this.todosLosProductos];
      return;
    }

    this.productos = this.todosLosProductos.filter(p =>
      p.nombreCategoria === this.categoriaActual!.nombre
    );
  }

  aplicarFiltros(): void {
    let filtrados = this.categoriaActual
      ? this.todosLosProductos.filter(p => p.nombreCategoria === this.categoriaActual!.nombre)
      : [...this.todosLosProductos];

    // Búsqueda
    if (this.busqueda.trim()) {
      const term = this.busqueda.toLowerCase();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term)
      );
    }

    // Precio máximo
    filtrados = filtrados.filter(p => p.precioMinimo <= this.precioMax);

    // Solo en stock
    if (this.soloEnStock) {
      filtrados = filtrados.filter(p => p.stockTotal > 0);
    }

    this.productos = filtrados;
  }

  limpiarFiltros(): void {
    this.busqueda = '';
    this.precioMax = 5000;
    this.soloEnStock = false;
    this.filtrarPorCategoria();
    this.aplicarFiltros();
  }

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