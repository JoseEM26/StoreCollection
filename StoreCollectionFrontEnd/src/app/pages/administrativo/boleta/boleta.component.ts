import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BoletaService } from '../../../service/boleta.service';
import { BoletaPageResponse, BoletaResponse } from '../../../model/boleta.model';
import Swal from 'sweetalert2';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-boleta',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './boleta.component.html',
  styleUrls: ['./boleta.component.css']
})
export class BoletaComponent implements OnInit {
  boletas: BoletaResponse[] = [];
  boletaSeleccionada: BoletaResponse | null = null;

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
      PENDIENTE: 'volver a pendiente',
      ATENDIDA: 'marcar como atendido',
      CANCELADA: 'cancelar'
    };

    const result = await Swal.fire({
      title: '¿Confirmar acción?',
      text: `Vas a ${textos[nuevoEstado]} el pedido #${boleta.id}`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, continuar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33'
    });

    if (!result.isConfirmed) return;

    this.isLoading = true;

    let serviceCall: Observable<BoletaResponse>;

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

        Swal.fire('¡Éxito!', `El pedido #${actualizada.id} ha sido actualizado.`, 'success');
      },
    error: (err) => {
  this.isLoading = false;
  const mensajeError = err.message ?? 'No se pudo cambiar el estado del pedido.';
  this.errorMessage = mensajeError;
  Swal.fire({
    title: 'Error',
    text: mensajeError,
    icon: 'error',
    confirmButtonText: 'Entendido'
  });
}
    });
  }

  descargarFactura(id: number): void {
    this.boletaService.descargarFacturaPdf(id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `factura_pedido_${id}.pdf`;
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        Swal.fire('¡Descargado!', 'La factura se ha guardado correctamente.', 'success');
      },
      error: (err: Error) => {
        Swal.fire('Error', err.message || 'No se pudo generar la factura.', 'error');
      }
    });
  }

  getEstadoBadge(estado: string): { text: string; class: string } {
    return {
      'PENDIENTE': { text: 'Pendiente', class: 'bg-warning text-dark' },
      'ATENDIDA':  { text: 'Atendida',  class: 'bg-success text-white' },
      'CANCELADA': { text: 'Cancelada', class: 'bg-danger text-white' }
    }[estado] || { text: estado, class: 'bg-secondary text-white' };
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