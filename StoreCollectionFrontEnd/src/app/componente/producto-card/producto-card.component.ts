// src/app/componente/producto-card/producto-card.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// Usa la interfaz real que te llega del backend
import { Producto } from '../../model';

@Component({
  selector: 'app-producto-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './producto-card.component.html',
  styleUrls: ['./producto-card.component.css']
})
export class ProductoCardComponent {
  @Input({ required: true }) producto!: Producto;

  // Precio más bajo de las variantes (o precio base si no hay variantes)
  get precio(): number {
    if (this.producto.variantes && this.producto.variantes.length > 0) {
      return Math.min(...this.producto.variantes.map(v => v.precio));
    }
    return 0; // o un precio por defecto
  }

  // Stock total (suma de todas las variantes)
  get stock(): number {
    if (this.producto.variantes && this.producto.variantes.length > 0) {
      return this.producto.variantes.reduce((total, v) => total + (v.activo ? v.stock : 0), 0);
    }
    return 0;
  }

  get hayStock(): boolean {
    return this.stock > 0;
  }

  // Imagen principal: la primera variante activa o una por defecto
  get imagenUrl(): string {
    if (this.producto.variantes && this.producto.variantes.length > 0) {
      const varianteConImagen = this.producto.variantes.find(v => v.imagenUrl && v.activo);
      if (varianteConImagen?.imagenUrl) {
        return varianteConImagen.imagenUrl;
      }
    }
    // Imagen por defecto si no hay
    return 'https://img.freepik.com/vector-premium/no-hay-fotos-ilustracion-plana_120816-197113.jpg';
  }

  // Nombre de categoría
  get nombreCategoria(): string {
    return this.producto.categoria?.nombre || 'Sin categoría';
  }
}