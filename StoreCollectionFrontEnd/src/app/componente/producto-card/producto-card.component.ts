// src/app/componente/producto-card/producto-card.component.ts
import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProductoPublic } from '../../model/index.dto';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-producto-card',
  standalone: true,
  imports: [RouterModule,CommonModule], // ← Solo RouterModule (ya no necesitas CommonModule)
  templateUrl: './producto-card.component.html',
  styleUrls: ['./producto-card.component.css']
})
export class ProductoCardComponent {
  @Input({ required: true }) producto!: ProductoPublic;

  get hayStock(): boolean {
    return this.producto.stockTotal > 0;
  }

  get imagenUrl(): string {
    return this.producto.imagenPrincipal || 'https://via.placeholder.com/300x300.png?text=Sin+Imagen';
  }

  get precio(): number {
    return this.producto.precioMinimo;
  }

  get nombreCategoria(): string {
    return this.producto.nombreCategoria;
  }

  get stock(): number {
    return this.producto.stockTotal;
  }
}