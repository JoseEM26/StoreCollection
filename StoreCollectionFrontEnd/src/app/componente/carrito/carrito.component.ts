import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import Swal from 'sweetalert2';

import { CarritoService } from '../../service/carrito.service';
import { CarritoItemResponse } from '../../model/carrito.model';
import { CheckoutFormComponent } from "./checkout-form/checkout-form.component";
import { TiendaService } from '../../service/tienda.service';

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
  private tiendaService = inject(TiendaService);

  items: CarritoItemResponse[] = [];
  totalItems = 0;
  totalPrecio = 0;
  loading = true;
  tienda: any = null;

  isProcessingOnline = false;
  isProcessingWhatsapp = false;

  tiendaId: number = 1; // TODO: obtener dinámicamente (desde servicio o store)

  // Control del modal de checkout
  showCheckoutModal = false;

  ngOnInit(): void {
  this.tienda = this.tiendaService.currentTiendaValue;
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

  // ── Métodos auxiliares con SweetAlert bonito ────────────────────────
  private showSuccess(title: string, text: string): void {
    Swal.fire({
      icon: 'success',
      title,
      text,
      timer: 3000,
      timerProgressBar: true,
      showConfirmButton: false,
      toast: true,
      position: 'top-end'
    });
  }

  private showError(title: string, text: string): void {
    Swal.fire({
      icon: 'error',
      title,
      text,
      confirmButtonColor: '#d33'
    });
  }

  private showWarning(title: string, text: string): void {
    Swal.fire({
      icon: 'warning',
      title,
      text,
      confirmButtonColor: '#fd7e14'
    });
  }

  private async showConfirm(title: string, text: string, confirmText: string = 'Sí'): Promise<boolean> {
    const result = await Swal.fire({
      title,
      text,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: confirmText,
      cancelButtonText: 'Cancelar'
    });
    return result.isConfirmed;
  }

  // ── Acciones del carrito ────────────────────────────────────────────
  async eliminarItem(itemId: number): Promise<void> {
    const confirmed = await this.showConfirm(
      '¿Eliminar producto?',
      'No podrás revertir esta acción',
      'Sí, eliminar'
    );

    if (!confirmed) return;

    this.carritoService.eliminarItem(itemId).subscribe({
      next: () => this.showSuccess('Eliminado', 'El producto fue removido del carrito'),
      error: () => this.showError('Error', 'No pudimos eliminar el producto')
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
      next: () => this.showSuccess('Actualizado', 'Cantidad modificada correctamente'),
      error: () => this.showError('Error', 'No pudimos actualizar la cantidad')
    });
  }

  async vaciarCarrito(): Promise<void> {
    const confirmed = await this.showConfirm(
      '¿Vaciar carrito completo?',
      'Se eliminarán todos los productos',
      'Sí, vaciar todo'
    );

    if (!confirmed) return;

    this.carritoService.vaciarCarrito().subscribe({
      next: () => this.showSuccess('¡Carrito vacío!', 'Todos los productos han sido eliminados'),
      error: () => this.showError('Error', 'No pudimos vaciar el carrito')
    });
  }

  // ── Checkout Online con formulario modal ────────────────────────────
  abrirFormularioCheckout(): void {
    if (this.items.length === 0) {
      this.showWarning('Carrito vacío', 'Agrega productos antes de continuar');
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

  Swal.fire({
    title: 'Procesando tu pedido...',
    allowOutsideClick: false,
    didOpen: () => Swal.showLoading()
  });

  this.carritoService.checkoutOnline(this.tiendaId, datosComprador).subscribe({
    next: (boleta) => {
      Swal.close();
      Swal.fire({
        icon: 'success',
        title: '¡Pedido confirmado!',
        html: `Tu boleta <strong>#${boleta.id}</strong> fue creada exitosamente.<br><br>Te contactaremos pronto por WhatsApp y email.`,
        confirmButtonColor: '#198754',
        timer: 5000,
        timerProgressBar: true
      });
    },
    error: (err) => {
      Swal.close();

      if (err.type === 'missing_email_config') {
        Swal.fire({
          icon: 'warning',
          title: '¡Configura tu correo primero!',
          html: `
            <p>${err.message}</p>
            <p class="mt-3 fw-bold text-danger">Sin esta configuración, no podemos enviar confirmaciones automáticas.</p>
            <p class="mt-3">¿Quieres configurarlo ahora?</p>
          `,
          showCancelButton: true,
          confirmButtonColor: '#6366f1',
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sí, configurar ahora',
          cancelButtonText: 'Más tarde',
          allowOutsideClick: false
        }).then(result => {
          if (result.isConfirmed) {
            // Redirige a edición de tienda (ajusta la ruta)
            window.location.href = `/admin/tiendas/editar/${this.tiendaId}`;
            // Mejor: usa Router si lo tienes injectado
            // this.router.navigate(['/admin/tiendas/editar', this.tiendaId]);
          }
        });
      } else {
        Swal.fire({
          icon: 'error',
          title: 'Error al procesar',
          text: err.message || 'Intenta de nuevo o contacta soporte',
          confirmButtonColor: '#d33'
        });
      }
    },
    complete: () => this.isProcessingOnline = false
  });
}

  // ── Checkout WhatsApp ───────────────────────────────────────────────
  async checkoutWhatsapp(): Promise<void> {
    if (this.items.length === 0) {
      this.showWarning('Carrito vacío', 'Agrega productos antes de continuar');
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

    Swal.fire({
      title: 'Generando enlace de WhatsApp...',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    let whatsappUrl: string;
    try {
      whatsappUrl = await firstValueFrom(
        this.carritoService.checkoutWhatsapp(this.tiendaId, datosComprador)
      );
    } catch (err: any) {
      Swal.close();
      this.showError('Error generando enlace', 'No pudimos preparar el mensaje para WhatsApp');
      this.isProcessingWhatsapp = false;
      return;
    }

    Swal.close();

    const confirmed = await this.showConfirm(
      '¿Enviar por WhatsApp?',
      'Se abrirá el chat directo con la tienda para coordinar el pedido',
      'Sí, abrir WhatsApp'
    );

    if (!confirmed) {
      this.isProcessingWhatsapp = false;
      return;
    }

    const newWindow = window.open(whatsappUrl, '_blank', 'noopener,noreferrer');

    if (newWindow) {
      newWindow.focus();
      this.showSuccess('¡Redirigiendo!', 'Abriendo chat de WhatsApp...');
    } else {
      Swal.fire({
        icon: 'warning',
        title: 'Ventana bloqueada',
        html: `
          Tu navegador bloqueó la ventana emergente.<br><br>
          <strong>Haz clic aquí para abrir el chat:</strong><br>
          <a href="${whatsappUrl}" target="_blank" rel="noopener noreferrer" class="btn btn-success mt-3">
            Abrir WhatsApp ahora
          </a>
        `,
        showConfirmButton: false
      });
    }

    this.isProcessingWhatsapp = false;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}