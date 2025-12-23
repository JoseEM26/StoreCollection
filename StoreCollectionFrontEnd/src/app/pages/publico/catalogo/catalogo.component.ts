// src/app/pages/publico/catalogo/catalogo.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, combineLatest } from 'rxjs';

import { Categoria } from '../../../model';
import { ProductoPublic } from '../../../model/index.dto';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { CategoriaPublicService } from '../../../service/categoria-public.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule], // ← Ya no importamos ProductoCardComponent
  templateUrl: './catalogo.component.html',
  styleUrls: ['./catalogo.component.css']
})
// src/app/pages/publico/catalogo/catalogo.component.ts

// ... imports anteriores ...

export class CatalogoComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  productos: ProductoPublic[] = [];
  todosLosProductos: ProductoPublic[] = [];
  categorias: Categoria[] = [];
  categoriaActual: Categoria | null = null;

  // Filtros
  busqueda = '';
  precioMax = 5000; // Valor inicial temporal, se actualizará
  soloEnStock = false;

  loading = true;

  // Nuevo: máximo real calculado
  private maxPrecioCalculado = 5000;

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

        // Calculamos el precio máximo real y actualizamos el filtro
        this.actualizarPrecioMaximo();

        this.filtrarPorCategoria();
        this.aplicarFiltros();
        this.loading = false;
      });
  }

  private filtrarPorCategoria(): void {
    if (!this.categoriaActual) {
      this.productos = [...this.todosLosProductos];
    } else {
      this.productos = this.todosLosProductos.filter(p =>
        p.nombreCategoria === this.categoriaActual!.nombre
      );
    }

    // Recalculamos el precio máximo cuando cambia la categoría
    this.actualizarPrecioMaximo();
  }

  aplicarFiltros(): void {
    let filtrados = this.categoriaActual
      ? this.todosLosProductos.filter(p => p.nombreCategoria === this.categoriaActual!.nombre)
      : [...this.todosLosProductos];

    if (this.busqueda.trim()) {
      const term = this.busqueda.toLowerCase();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term)
      );
    }

    // Aplicamos el precio máximo actual (el usuario puede haberlo movido)
    filtrados = filtrados.filter(p => p.precioMinimo <= this.precioMax);

    if (this.soloEnStock) {
      filtrados = filtrados.filter(p => p.stockTotal > 0);
    }

    this.productos = filtrados;

    // Opcional: recalcular max si se aplican filtros fuertes (ej: solo en stock o búsqueda)
    // this.actualizarPrecioMaximo();
  }

  limpiarFiltros(): void {
    this.busqueda = '';
    this.soloEnStock = false;
    this.actualizarPrecioMaximo(); // Restablecemos al máximo real
    this.filtrarPorCategoria();
    this.aplicarFiltros();
  }

  /**
   * Calcula el precio máximo real de los productos visibles
   * y lo redondea hacia arriba (ej: 1299 → 1300, 1350 → 1400)
   */
  private actualizarPrecioMaximo(): void {
    const productosVisibles = this.categoriaActual
      ? this.todosLosProductos.filter(p => p.nombreCategoria === this.categoriaActual!.nombre)
      : this.todosLosProductos;

    if (productosVisibles.length === 0) {
      this.maxPrecioCalculado = 5000;
      this.precioMax = 5000;
      return;
    }

    const maxPrecio = Math.max(...productosVisibles.map(p => p.precioMinimo));

    // Redondeamos hacia arriba de forma bonita
    const redondeado = Math.ceil(maxPrecio / 100) * 100; // Ej: 1299 → 1300, 1301 → 1400

    this.maxPrecioCalculado = Math.max(redondeado, 500); // Mínimo razonable
    this.precioMax = this.maxPrecioCalculado; // Mostrar todo por defecto
  }

  // Getter para usar en el template
  get precioMaximoRango(): number {
    return this.maxPrecioCalculado;
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