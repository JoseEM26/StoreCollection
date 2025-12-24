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

  // ID de la tienda (debes pasarlo desde donde abras el carrito, ej: desde el catálogo)
  tiendaId: number = 1; // Cambia esto dinámicamente según la tienda actual

  constructor(private carritoService: CarritoService) {}

  ngOnInit(): void {
    this.carritoService.carritoItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items || [];
        this.totalItems = this.carritoService.getTotalItems();
        this.totalPrecio = this.carritoService.getTotalPrecio();
        this.loading = false;
      });
  }

  eliminarItem(itemId: number): void {
    if (confirm('¿Eliminar este producto del carrito?')) {
      this.carritoService.eliminarItem(itemId).subscribe({
        error: () => alert('Error al eliminar el producto')
      });
    }
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
    if (confirm('¿Vaciar todo el carrito?')) {
      this.carritoService.vaciarCarrito().subscribe();
    }
  }

  checkoutOnline(): void {
    if (this.items.length === 0) return;

    this.carritoService.checkoutOnline(this.tiendaId).subscribe({
      next: (boleta) => {
        alert(`¡Pedido registrado! Nº ${boleta.id}\nTe contactaremos pronto para confirmar.`);
      },
      error: (err) => {
        console.error(err);
        alert('Error al registrar el pedido. Intenta nuevamente.');
      }
    });
  }

  checkoutWhatsapp(): void {
    if (this.items.length === 0) return;

    this.carritoService.checkoutWhatsapp(this.tiendaId).subscribe({
      next: (url) => {
        window.open(url, '_blank');
      },
      error: () => {
        alert('Error al generar el mensaje de WhatsApp.');
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}