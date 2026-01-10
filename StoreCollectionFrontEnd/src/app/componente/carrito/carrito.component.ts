import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { CarritoService } from '../../service/carrito.service';
import { CarritoItemResponse } from '../../model/carrito.model';
import { CheckoutFormComponent } from "./checkout-form/checkout-form.component";
import { TiendaService } from '../../service/tienda.service';
import { SwalService } from '../../service/SweetAlert/swal.service';
import Swal from 'sweetalert2';

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
  private swal = inject(SwalService);

  items: CarritoItemResponse[] = [];
  loading = true;
  tienda: any = null;

  isProcessingOnline = false;
  isProcessingWhatsapp = false;

  // Getter que obtiene el ID real de la tienda actual
  get tiendaId(): number {
    return this.tienda?.id ?? 1; // fallback desarrollo
  }

  // Control del modal
  showCheckoutModal = false;

  // Getters para el template (siempre frescos desde el servicio)
  get totalItems(): number {
    return this.carritoService.getTotalItemsSync();
  }

  get totalPrecio(): number {
    return this.carritoService.getTotalPrecioSync();
  }

  ngOnInit(): void {
    this.tienda = this.tiendaService.currentTiendaValue;

    // Cambio de tienda → recargar carrito
    this.tiendaService.currentTienda$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tienda => {
        if (tienda?.id) {
          this.tienda = tienda;
          this.carritoService.cargarCarritoDesdeBackend();
        }
      });

    // Suscripción principal al carrito
    this.carritoService.carritoItems$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.items = items || [];
        this.loading = false;
      });

    // Carga inicial
    if (this.tienda?.id) {
      this.carritoService.cargarCarritoDesdeBackend();
    }
  }

  // ── Acciones del carrito ────────────────────────────────────────────────

  async eliminarItem(itemId: number): Promise<void> {
    const item = this.items.find(i => i.id === itemId);
    if (!item) return;

    const result = await this.swal.confirmDelete(
      item.nombreProducto,
      'El producto se quitará del carrito.'
    );

    if (!result.isConfirmed) return;

    this.carritoService.eliminarItem(itemId).subscribe({
      next: () => {
        this.swal.toast('Producto eliminado correctamente', 'success');
      },
      error: () => {
        this.swal.error('No se pudo eliminar', 'Intenta nuevamente');
      }
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

    this.carritoService.actualizarCantidad(item.id, nuevaCantidad, item.varianteId ?? 0)
      .subscribe({
        next: () => {
          this.swal.toast('Cantidad actualizada', 'success', 2000);
        },
        error: () => {
          this.swal.error('Error al actualizar cantidad');
        }
      });
  }

  async vaciarCarrito(): Promise<void> {
    const result = await this.swal.confirmAction({
      title: '¿Vaciar carrito completo?',
      text: 'Se eliminarán todos los productos seleccionados',
      confirmButtonText: 'Sí, vaciar todo',
      icon: 'warning'
    });

    if (!result.isConfirmed) return;

    this.carritoService.vaciarCarrito().subscribe({
      next: () => {
        this.swal.success('¡Carrito vacío!', 'Todos los productos han sido eliminados');
      },
      error: () => {
        this.swal.error('No pudimos vaciar el carrito', 'Intenta de nuevo');
      }
    });
  }

  // ── Checkout Online ─────────────────────────────────────────────────────

  abrirFormularioCheckout(): void {
    if (this.totalItems === 0) {
      this.swal.warning('Carrito vacío', 'Agrega productos antes de continuar');
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

  this.swal.loading('Procesando tu pedido...');

  this.carritoService.checkoutOnline(datosComprador).subscribe({
    next: (boleta) => {
      this.swal.close();
      this.swal.success(
        '¡Pedido confirmado!',
        `Tu boleta <strong>#${boleta.id}</strong> fue creada exitosamente.<br><br>Te contactaremos pronto por WhatsApp y email.`
      );
    },
    error: (err: any) => {
      this.swal.close();

      if (err.type === 'missing_email_config') {
        Swal.fire({
          icon: 'warning',
          title: '¡Configura tu correo primero!',
          html: `
            ${err.message || 'Es necesario configurar el correo para enviar confirmaciones automáticas.'}
            <br><br>
            Sin esta configuración no podemos enviar confirmaciones automáticas.<br>
            ¿Quieres configurarlo ahora?
          `,
          showCancelButton: true,
          confirmButtonColor: '#fd7e14',
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sí, configurar ahora',
          cancelButtonText: 'Más tarde'
        }).then(result => {
          if (result.isConfirmed) {
            window.location.href = `/admin/tiendas/editar/${this.tiendaId}`;
          }
        });
      } else {
        this.swal.error('Error al procesar', err.message || 'Intenta de nuevo');
      }
    },
    complete: () => this.isProcessingOnline = false
  });
}

  // ── Checkout WhatsApp ──────────────────────────────────────────────────

  async checkoutWhatsapp(): Promise<void> {
    if (this.totalItems === 0) {
      this.swal.warning('Carrito vacío', 'Agrega productos antes de continuar');
      return;
    }

    if (this.isProcessingOnline || this.isProcessingWhatsapp) return;

    // ← Aquí deberías tener los datos reales del usuario
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

    const loadingRef = this.swal.loading('Generando enlace de WhatsApp...');

    try {
      const whatsappUrl = await this.carritoService.checkoutWhatsapp(datosComprador).toPromise();

      this.swal.close();

      const confirmed = await this.swal.confirmAction({
        title: '¿Enviar por WhatsApp?',
        text: 'Se abrirá el chat directo con la tienda',
        confirmButtonText: 'Sí, abrir WhatsApp',
        icon: 'info'
      });

      if (!confirmed.isConfirmed) {
        this.isProcessingWhatsapp = false;
        return;
      }

      const newWindow = window.open(whatsappUrl, '_blank', 'noopener,noreferrer');

      if (newWindow) {
        newWindow.focus();
        this.swal.toast('Abriendo chat de WhatsApp...', 'success', 2500);
      } else {
        await this.swal.warning(
          'Ventana bloqueada',
          `Tu navegador bloqueó la ventana emergente.<br><br>
           <a href="${whatsappUrl}" target="_blank" rel="noopener noreferrer">Haz clic aquí para abrir WhatsApp</a>`
        );
      }
    } catch (err) {
      this.swal.close();
      this.swal.error('Error generando enlace', 'No pudimos preparar el mensaje');
    } finally {
      this.isProcessingWhatsapp = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}