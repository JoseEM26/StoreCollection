import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import Swal from 'sweetalert2';

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
  private carritoService = inject(CarritoService);

  items: CarritoItemResponse[] = [];
  totalItems = 0;
  totalPrecio = 0;
  loading = true;

  // Estados de carga para evitar doble click
  isProcessingOnline = false;
  isProcessingWhatsapp = false;

  tiendaId: number = 1; // TODO: obtener dinámicamente

  ngOnInit(): void {
    this.carritoService.carritoItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items || [];
        this.totalItems = this.carritoService.getTotalItemsSync();
        this.totalPrecio = this.carritoService.getTotalPrecioSync();
        this.loading = false;
      });

    this.carritoService.cargarCarritoDesdeBackend();
  }

  // ── Métodos auxiliares ──────────────────────────────────────────
  private showError(message: string = 'Ocurrió un error inesperado'): void {
    Swal.fire({
      icon: 'error',
      title: '¡Ups!',
      text: message,
      timer: 3500
    });
  }

  private showSuccess(title: string, message: string): void {
    Swal.fire({
      icon: 'success',
      title,
      text: message,
      timer: 3500,
      showConfirmButton: false
    });
  }

  // ── Acciones ────────────────────────────────────────────────────
  async eliminarItem(itemId: number): Promise<void> {
    const { isConfirmed } = await Swal.fire({
      title: '¿Eliminar producto?',
      text: "No podrás revertir esto",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    });

    if (!isConfirmed) return;

    this.carritoService.eliminarItem(itemId).subscribe({
      error: () => this.showError('No pudimos eliminar el producto')
    });
  }

  async actualizarCantidad(itemId: number, cambio: number): Promise<void> {
    const item = this.items.find(i => i.id === itemId);
    if (!item) return;

    const nuevaCantidad = item.cantidad + cambio;

    if (nuevaCantidad < 1) {
      await this.eliminarItem(itemId);
      return;
    }

    this.carritoService.actualizarCantidad(itemId, nuevaCantidad).subscribe({
      error: () => this.showError('No pudimos actualizar la cantidad')
    });
  }

  async vaciarCarrito(): Promise<void> {
    const { isConfirmed } = await Swal.fire({
      title: '¿Vaciar carrito?',
      text: 'Se eliminarán todos los productos',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sí, vaciar',
      cancelButtonText: 'Cancelar'
    });

    if (!isConfirmed) return;

    this.carritoService.vaciarCarrito().subscribe({
      next: () => this.showSuccess('¡Listo!', 'El carrito ha sido vaciado'),
      error: () => this.showError('No pudimos vaciar el carrito')
    });
  }

  async checkoutOnline(): Promise<void> {
    if (this.items.length === 0) {
      Swal.fire({ icon: 'info', title: 'Carrito vacío', timer: 2200 });
      return;
    }

    if (this.isProcessingOnline || this.isProcessingWhatsapp) return;

    const { isConfirmed } = await Swal.fire({
      title: '¿Confirmar pedido online?',
      text: 'Se registrará y te contactaremos pronto',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#198754',
      confirmButtonText: 'Sí, confirmar',
      cancelButtonText: 'Cancelar'
    });

    if (!isConfirmed) return;

    this.isProcessingOnline = true;

    this.carritoService.checkoutOnline(this.tiendaId).subscribe({
      next: (boleta) => {
        this.showSuccess('¡Pedido registrado!', `Número: #${boleta.id}\nTe contactaremos pronto`);
        // El servicio ya limpia el carrito vía tap()
      },
      error: (err) => {
        console.error('Error checkout online:', err);
        this.showError('No pudimos procesar el pedido');
      },
      complete: () => {
        this.isProcessingOnline = false;
      }
    });
  }

  async checkoutWhatsapp(): Promise<void> {
    if (this.items.length === 0) {
      Swal.fire({ icon: 'info', title: 'Carrito vacío', timer: 2200 });
      return;
    }

    if (this.isProcessingOnline || this.isProcessingWhatsapp) return;

    // Generar el URL ANTES de la confirmación para minimizar el delay y reducir bloqueo de popup
    this.isProcessingWhatsapp = true;
    let whatsappUrl: string;
    try {
      whatsappUrl = await firstValueFrom(this.carritoService.checkoutWhatsapp(this.tiendaId));
    } catch (err) {
      console.error('Error generando URL WhatsApp:', err);
      this.showError('No pudimos generar el enlace de WhatsApp');
      this.isProcessingWhatsapp = false;
      return;
    }
    this.isProcessingWhatsapp = false;

    const { isConfirmed } = await Swal.fire({
      title: '¿Enviar por WhatsApp?',
      text: 'Se abrirá el chat con la tienda',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#25d366',
      confirmButtonText: 'Sí, enviar ahora',
      cancelButtonText: 'Cancelar'
    });

    if (!isConfirmed) return;

    this.isProcessingWhatsapp = true;

    const newWindow = window.open(whatsappUrl, '_blank', 'noopener,noreferrer');

    if (newWindow) {
      newWindow.focus();
      this.showSuccess('¡Abriendo WhatsApp!', 'Redirigiendo al chat...');
    } else {
      // Fallback con enlace clicable
      Swal.fire({
        title: 'No se pudo abrir automáticamente',
        html: `
          El navegador bloqueó la ventana.<br><br>
          <strong>Haz clic aquí:</strong><br>
          <a href="${whatsappUrl}" target="_blank" rel="noopener noreferrer" style="color:#25d366; font-weight:600; word-break:break-all;">
            Abrir chat de WhatsApp →
          </a>
        `,
        showConfirmButton: false,
        footer: '<small>Recomendación: permite ventanas emergentes</small>'
      });
    }

    this.isProcessingWhatsapp = false;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}