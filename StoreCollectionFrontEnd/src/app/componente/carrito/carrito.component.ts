import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { CarritoService } from '../../service/carrito.service';
import { CarritoItemResponse } from '../../model/carrito.model';
import { TiendaService } from '../../service/tienda.service';
import { SwalService } from '../../service/SweetAlert/swal.service';
import Swal from 'sweetalert2';

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
  private tiendaService = inject(TiendaService);
  private swal = inject(SwalService);

  items: CarritoItemResponse[] = [];
  loading = true;
  tienda: any = null; // Idealmente tipar como Tienda | null
  isProcessingWhatsapp = false;

  get tiendaId(): number {
    return this.tienda?.id ?? 0; // 0 como fallback (mejor validar antes de usarlo)
  }

  get totalItems(): number {
    return this.carritoService.getTotalItemsSync();
  }

  get totalPrecio(): number {
    return this.carritoService.getTotalPrecioSync();
  }

  ngOnInit(): void {
    // Suscribirse a cambios de tienda
    this.tiendaService.currentTienda$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tienda => {
        this.tienda = tienda;
        if (tienda?.id) {
          this.carritoService.cargarCarritoDesdeBackend();
        } else {
          this.items = [];
          this.loading = false;
        }
      });

    // Suscribirse a cambios en los items del carrito
    this.carritoService.carritoItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items || [];
        this.loading = false;
      });

    // Carga inicial
    this.tienda = this.tiendaService.currentTiendaValue;
    if (this.tienda?.id) {
      this.carritoService.cargarCarritoDesdeBackend();
    } else {
      this.loading = false;
    }
  }

  async eliminarItem(itemId: number): Promise<void> {
    const item = this.items.find(i => i.id === itemId);
    if (!item) return;

    const result = await this.swal.confirmDelete(
      item.nombreProducto,
      'El producto se quitará del carrito.'
    );

    if (!result.isConfirmed) return;

    this.carritoService.eliminarItem(itemId).subscribe({
      next: () => this.swal.toast('Producto eliminado', 'success'),
      error: () => this.swal.error('No se pudo eliminar el producto')
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

    // Validación extra de varianteId
    if (!item.varianteId) {
      this.swal.error('No se puede actualizar: falta información del producto');
      return;
    }

    this.carritoService.actualizarCantidad(item.id, nuevaCantidad, item.varianteId)
      .subscribe({
        next: () => this.swal.toast('Cantidad actualizada', 'success', 2000),
        error: () => this.swal.error('Error al actualizar cantidad')
      });
  }

  async vaciarCarrito(): Promise<void> {
    const result = await this.swal.confirmAction({
      title: '¿Vaciar carrito?',
      text: 'Se eliminarán todos los productos',
      confirmButtonText: 'Sí, vaciar',
      icon: 'warning'
    });

    if (!result.isConfirmed) return;

    this.carritoService.vaciarCarrito().subscribe({
      next: () => this.swal.success('Carrito vacío', 'Todos los productos eliminados'),
      error: () => this.swal.error('No pudimos vaciar el carrito')
    });
  }

  // ================= CHECKOUT WHATSAPP =================
  async enviarPedidoWhatsapp(): Promise<void> {
    if (this.totalItems === 0) {
      this.swal.warning('Carrito vacío', 'Agrega productos antes de continuar');
      return;
    }

    if (!this.tienda?.id) {
      this.swal.warning('Tienda no seleccionada', 'Selecciona una tienda primero');
      return;
    }

    if (this.isProcessingWhatsapp) return;

    this.isProcessingWhatsapp = true;

    Swal.fire({
      title: 'Preparando pedido...',
      text: 'Generando mensaje para WhatsApp',
      allowOutsideClick: false,
      allowEscapeKey: false,
      didOpen: () => Swal.showLoading()
    });

    try {
      const whatsappUrl = await this.carritoService
        .checkoutWhatsappSimple() // ← Método recomendado que ya no necesita parámetros
        .toPromise();

      Swal.close();

      const confirmed = await this.swal.confirmAction({
        title: '¿Enviar pedido por WhatsApp?',
        text: `Se abrirá WhatsApp con tu pedido listo para enviar a ${this.tienda?.nombre || 'la tienda'}`,
        confirmButtonText: 'Sí, abrir WhatsApp',
        icon: 'success'
      });

      if (!confirmed.isConfirmed) {
        this.isProcessingWhatsapp = false;
        return;
      }

      const newWindow = window.open(whatsappUrl, '_blank', 'noopener,noreferrer');
      if (newWindow) {
        newWindow.focus();
        this.swal.toast('Abriendo WhatsApp...', 'success');
      } else {
        // Fallback para bloqueo de popups
        await Swal.fire({
          title: 'Ventana bloqueada',
          html: `
            <p>No se pudo abrir WhatsApp automáticamente.</p>
            <p>Haz clic en el siguiente enlace:</p>
            <a href="${whatsappUrl}" target="_blank" class="btn btn-success mt-3">
              <i class="bi bi-whatsapp"></i> Abrir WhatsApp
            </a>
          `,
          icon: 'warning',
          showConfirmButton: false
        });
      }
    } catch (err) {
      console.error('Error al generar pedido WhatsApp:', err);
      Swal.close();
      this.swal.error('Error al preparar el pedido', 'Inténtalo de nuevo más tarde');
    } finally {
      this.isProcessingWhatsapp = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}