// src/app/pages/publico/catalogo/catalogo.component.ts
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { ProductosService } from '../../../service/productos.service';
import { Producto } from '../../../model/producto.model';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ProductoCardComponent],
  templateUrl: './catalogo.component.html',
  styleUrls: ['./catalogo.component.css']
})
export class CatalogoComponent {
  private route = inject(ActivatedRoute);
  private service = inject(ProductosService);

  categoria = this.route.snapshot.paramMap.get('categoria');
  todosProductos: Producto[] = [];
  productos: Producto[] = [];

  // Filtros
  busqueda = '';
  precioMax = 5000;
  soloEnStock = false;

  constructor() {
    this.todosProductos = this.categoria
      ? this.service.getByCategoria(this.categoria)
      : this.service.getAll();

    this.productos = [...this.todosProductos];
    this.aplicarFiltros();
  }

  get tituloPagina(): string {
    if (this.categoria) {
      const nombres = {
        celulares: 'Celulares',
        laptops: 'Laptops',
        moda: 'Moda y Ropa',
        electronica: 'Electrónica',
        hogar: 'Hogar',
        deportes: 'Deportes',
        belleza: 'Belleza',
        juguetes: 'Juguetes'
      };
      return nombres[this.categoria as keyof typeof nombres] || this.categoria;
    }
    return 'Todos los Productos';
  }

  get totalResultados(): number {
    return this.productos.length;
  }

  aplicarFiltros() {
    let filtrados = [...this.todosProductos];

    // Búsqueda por nombre
    if (this.busqueda.trim()) {
      const term = this.busqueda.toLowerCase();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term) ||
        p.descripcion.toLowerCase().includes(term)
      );
    }

    // Precio máximo
    filtrados = filtrados.filter(p => p.precio <= this.precioMax);

    // Solo en stock
    if (this.soloEnStock) {
      filtrados = filtrados.filter(p => p.stock > 0);
    }

    this.productos = filtrados;
  }

  limpiarFiltros() {
    this.busqueda = '';
    this.precioMax = 5000;
    this.soloEnStock = false;
    this.aplicarFiltros();
  }
}