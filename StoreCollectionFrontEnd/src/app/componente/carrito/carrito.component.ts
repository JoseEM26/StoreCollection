// src/app/componente/carrito/carrito.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { CarritoService } from '../../service/carrito.service';
import { CarritoItemResponse } from '../../model/carrito.model';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './carrito.component.html',
  styleUrl: './carrito.component.css'
})
export class CarritoComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  items: CarritoItemResponse[] = [];
  totalItems = 0;
  totalPrecio = 0;
  loading = true;

  constructor(private carritoService: CarritoService) {}

  ngOnInit(): void {
    this.carritoService.carritoItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items;
        this.totalItems = this.carritoService.getTotalItems();
        this.totalPrecio = this.carritoService.getTotalPrecio();
        this.loading = false;
      });
  }

  eliminarItem(itemId: number): void {
    this.carritoService.eliminarItem(itemId).subscribe({
      error: () => alert('Error al eliminar el producto')
    });
  }

  actualizarCantidad(itemId: number, nuevaCantidad: number): void {
    if (nuevaCantidad < 1) {
      this.eliminarItem(itemId);
      return;
    }

    this.carritoService.actualizarCantidad(itemId, nuevaCantidad).subscribe({
      error: () => alert('Error al actualizar cantidad')
    });
  }

  vaciarCarrito(): void {
    if (confirm('¿Estás seguro de vaciar el carrito?')) {
      this.carritoService.vaciarCarrito().subscribe();
    }
  }

  irAlCheckout(): void {
    // Aquí puedes redirigir a una página de checkout o abrir un modal
    alert('Funcionalidad de checkout en desarrollo. Próximamente podrás finalizar tu compra.');
    // router.navigate(['/', this.tienda.slug, 'checkout']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}