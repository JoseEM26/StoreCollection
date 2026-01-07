import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BoletaService } from '../../../service/boleta.service';
import { BoletaPageResponse, BoletaResponse } from '../../../model/boleta.model';
import Swal from 'sweetalert2';
import { Observable } from 'rxjs';
import { CrearVentaDirectaFormComponent } from "./crear-venta-directa-form/crear-venta-directa-form.component";

@Component({
  selector: 'app-boleta',
  standalone: true,
  imports: [CommonModule, FormsModule, CrearVentaDirectaFormComponent],
  templateUrl: './boleta.component.html',
  styleUrls: ['./boleta.component.css']
})
export class BoletaComponent implements OnInit {
  boletas: BoletaResponse[] = [];
  boletaSeleccionada: BoletaResponse | null = null;
mostrarModalVentaDirecta = false;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  selectedEstado: '' | 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA' = '';
  isLoading = false;
  errorMessage: string | null = null;

  constructor(
    private boletaService: BoletaService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarBoletas();
  }


// Métodos para abrir/cerrar el modal
abrirModalVentaDirecta(): void {
  this.mostrarModalVentaDirecta = true;
}

cerrarModalVentaDirecta(): void {
  this.mostrarModalVentaDirecta = false;
}
  cargarBoletas(page: number = this.currentPage): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.boletaService.getBoletasPaginadas(page, this.pageSize, 'fecha,desc', this.selectedEstado || undefined)
      .subscribe({
        next: (pageData: BoletaPageResponse) => {
          this.boletas = pageData.content ?? [];
          this.currentPage = pageData.number;
          this.totalPages = pageData.totalPages;
          this.totalElements = pageData.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.errorMessage = 'No se pudieron cargar los pedidos. Intenta nuevamente.';
          this.isLoading = false;
          Swal.fire('Error', this.errorMessage, 'error');
        }
      });
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.cargarBoletas(this.currentPage - 1);
  }

  paginaSiguiente(): void {
    if (this.currentPage < this.totalPages - 1) this.cargarBoletas(this.currentPage + 1);
  }

  get visiblePages(): number[] {
    const range = 2;
    const start = Math.max(0, this.currentPage - range);
    const end = Math.min(this.totalPages - 1, this.currentPage + range);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  filtrarPorEstado(): void {
    this.currentPage = 0;
    this.cargarBoletas(0);
  }

  verDetalle(boleta: BoletaResponse): void {
    this.boletaSeleccionada = { ...boleta };
  }

  cerrarDetalle(): void {
    this.boletaSeleccionada = null;
  }

async cambiarEstado(boleta: BoletaResponse, nuevoEstado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'): Promise<void> {
  const textos = {
    PENDIENTE: { verbo: 'volver a pendiente', titulo: 'Volver a pendiente' },
    ATENDIDA: { verbo: 'marcar como atendido', titulo: 'Marcar como atendida' },
    CANCELADA: { verbo: 'cancelar', titulo: 'Cancelar pedido' }
  };

  const result = await Swal.fire({
    title: textos[nuevoEstado].titulo,
    text: `¿Estás seguro de que deseas ${textos[nuevoEstado].verbo} el pedido #${boleta.id}?`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, confirmar',
    cancelButtonText: 'No, cancelar',
    confirmButtonColor: '#3085d6',
    cancelButtonColor: '#d33'
  });

  if (!result.isConfirmed) return;

  this.isLoading = true;

  let serviceCall: Observable<BoletaResponse>;

  // ← AQUÍ ESTÁ LA CORRECCIÓN
  switch (nuevoEstado) {
    case 'ATENDIDA':
      serviceCall = this.boletaService.marcarComoAtendida(boleta.id);
      break;
    case 'CANCELADA':
      serviceCall = this.boletaService.cancelarBoleta(boleta.id);
      break;
    case 'PENDIENTE':
      serviceCall = this.boletaService.volverAPendiente(boleta.id);
      break;
    default:
      this.isLoading = false;
      return;
  }

  serviceCall.subscribe({
    next: (actualizada) => {
      const index = this.boletas.findIndex(b => b.id === actualizada.id);
      if (index !== -1) this.boletas[index] = actualizada;

      if (this.boletaSeleccionada?.id === actualizada.id) {
        this.boletaSeleccionada = actualizada;
      }

      this.cdr.detectChanges();
      this.isLoading = false;

      Swal.fire({
        title: '¡Listo!',
        text: `El pedido #${actualizada.id} ha sido ${textos[nuevoEstado].verbo} correctamente.`,
        icon: 'success',
        timer: 3000,
        showConfirmButton: false
      });
    },
    error: (err) => {
      this.isLoading = false;
      let mensaje = err.message || 'No se pudo completar la acción.';

      if (mensaje.toLowerCase().includes('stock insuficiente')) {
        Swal.fire('Stock insuficiente', 'No hay suficiente stock para atender este pedido.', 'warning');
      } else {
        Swal.fire('Error', mensaje, 'error');
      }
    }
  });
}
  // =============================================
  // ENVÍO DE CONFIRMACIÓN POR WHATSAPP AL CLIENTE
  // =============================================
  enviarConfirmacionWhatsapp(boleta: BoletaResponse): void {
    Swal.fire({
      title: 'Enviando mensaje...',
      text: 'Generando enlace de WhatsApp para el cliente',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    this.boletaService.generarWhatsappConfirmacionCliente(boleta.id).subscribe({
      next: (whatsappUrl) => {
        Swal.close();
        window.open(whatsappUrl, '_blank');

        Swal.fire({
          title: '¡Enlace generado!',
          text: `Se abrió WhatsApp con el mensaje para el cliente del pedido #${boleta.id}`,
          icon: 'success',
          timer: 3000,
          showConfirmButton: false
        });
      },
      error: (err) => {
        Swal.fire({
          title: 'No se pudo enviar',
          text: err.message || 'El cliente no tiene un número de teléfono válido.',
          icon: 'error',
          confirmButtonText: 'Entendido'
        });
      }
    });
  }

  // =============================================
  // DESCARGA DE FACTURA PDF
  // =============================================
  descargarFactura(id: number): void {
    Swal.fire({
      title: 'Generando factura...',
      text: 'Por favor espera',
      allowOutsideClick: false,
      didOpen: () => {
        Swal.showLoading();
      }
    });

    this.boletaService.descargarFacturaPdf(id).subscribe({
      next: (blob: Blob) => {
        Swal.close();

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `factura_pedido_${id}.pdf`;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        Swal.fire({
          title: '¡Factura descargada!',
          text: `Se ha guardado el PDF del pedido #${id}`,
          icon: 'success',
          timer: 2500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        Swal.close();
        Swal.fire({
          title: 'Error',
          text: err.message || 'No se pudo generar la factura. Asegúrate de que el pedido esté atendido.',
          icon: 'error'
        });
      }
    });
  }

  // =============================================
  // UTILIDADES VISUALES
  // =============================================
  getEstadoBadge(estado: string): { text: string; class: string } {
    const badges: Record<string, { text: string; class: string }> = {
      'PENDIENTE': { text: 'Pendiente', class: 'bg-warning text-dark' },
      'ATENDIDA':  { text: 'Atendida',  class: 'bg-success text-white' },
      'CANCELADA': { text: 'Cancelada', class: 'bg-danger text-white' }
    };
    return badges[estado] || { text: estado, class: 'bg-secondary text-white' };
  }

  formatDate(fecha: string): string {
    return new Date(fecha).toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getTotalItems(detalles: any[] = []): number {
    return detalles.reduce((sum, item) => sum + item.cantidad, 0);
  }
}