import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import Swal from 'sweetalert2';

import { CarritoService } from '../../service/carrito.service';
import { CarritoItemResponse } from '../../model/carrito.model';
import { CheckoutFormComponent } from "./checkout-form/checkout-form.component";

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, RouterLink, CheckoutFormComponent],
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

  isProcessingOnline = false;
  isProcessingWhatsapp = false;

  tiendaId: number = 1; // TODO: obtener dinámicamente (desde servicio o store)

  // Control del modal de checkout
  showCheckoutModal = false;

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
    Swal.fire({ icon: 'error', title: '¡Ups!', text: message, timer: 3500 });
  }

  private showSuccess(title: string, message: string): void {
    Swal.fire({ icon: 'success', title, text: message, timer: 3500, showConfirmButton: false });
  }

  // ── Acciones del carrito ────────────────────────────────────────
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

  // ── Checkout Online con formulario modal ─────────────────────────
  abrirFormularioCheckout(): void {
    if (this.items.length === 0) {
      Swal.fire({ icon: 'info', title: 'Carrito vacío', timer: 2200 });
      return;
    }

    if (this.isProcessingOnline || this.isProcessingWhatsapp) return;

    this.showCheckoutModal = true;
  }

  cerrarFormularioCheckout(): void {
    this.showCheckoutModal = false;
  }

  procesarCheckout(datosComprador: any): void {
    this.showCheckoutModal = false;
    this.isProcessingOnline = true;

    this.carritoService.checkoutOnline(this.tiendaId, datosComprador).subscribe({
      next: (boleta) => {
        this.showSuccess('¡Pedido registrado!', `Número: #${boleta.id}\nTe contactaremos pronto`);
      },
      error: (err) => {
        console.error('Error checkout online:', err);
        this.showError(err.message || 'No pudimos procesar el pedido');
      },
      complete: () => {
        this.isProcessingOnline = false;
      }
    });
  }

  // ── Checkout WhatsApp (con datos de prueba por ahora) ────────────
  async checkoutWhatsapp(): Promise<void> {
    if (this.items.length === 0) {
      Swal.fire({ icon: 'info', title: 'Carrito vacío', timer: 2200 });
      return;
    }

    if (this.isProcessingOnline || this.isProcessingWhatsapp) return;

    const datosComprador = {
      compradorNombre: 'Cliente de Prueba',
      compradorEmail: 'prueba@ejemplo.com',
      compradorTelefono: '+51999123456',
      direccionEnvio: 'Av. Prueba 123, Miraflores',
      referenciaEnvio: 'Frente al parque',
      distrito: 'Miraflores',
      provincia: 'Lima',
      departamento: 'Lima',
      codigoPostal: '15074',
      tipoEntrega: 'DOMICILIO' as const
    };

    this.isProcessingWhatsapp = true;

    let whatsappUrl: string;
    try {
      whatsappUrl = await firstValueFrom(
        this.carritoService.checkoutWhatsapp(this.tiendaId, datosComprador)
      );
    } catch (err: any) {
      console.error('Error generando URL WhatsApp:', err);
      this.showError('No pudimos generar el enlace de WhatsApp');
      this.isProcessingWhatsapp = false;
      return;
    }

    const { isConfirmed } = await Swal.fire({
      title: '¿Enviar por WhatsApp?',
      text: 'Se abrirá el chat con la tienda',
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#25d366',
      confirmButtonText: 'Sí, enviar ahora',
      cancelButtonText: 'Cancelar'
    });

    if (!isConfirmed) {
      this.isProcessingWhatsapp = false;
      return;
    }

    const newWindow = window.open(whatsappUrl, '_blank', 'noopener,noreferrer');

    if (newWindow) {
      newWindow.focus();
      this.showSuccess('¡Abriendo WhatsApp!', 'Redirigiendo al chat...');
    } else {
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