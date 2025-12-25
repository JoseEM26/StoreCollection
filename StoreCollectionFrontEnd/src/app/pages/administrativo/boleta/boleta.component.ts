import { Component, OnInit, ChangeDetectorRef } from '@angular/core'; // ← Agrega ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BoletaService } from '../../../service/boleta.service';
import { BoletaPageResponse, BoletaResponse } from '../../../model/boleta.model';

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
    private cdr: ChangeDetectorRef  // ← Inyectamos para forzar detección de cambios
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
          this.errorMessage = 'Error al cargar los pedidos. Intenta nuevamente.';
          this.isLoading = false;
        }
      });
  }

  // Paginación (sin cambios)
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
    this.boletaSeleccionada = { ...boleta }; // ← Clonamos para evitar problemas de referencia
  }

  cerrarDetalle(): void {
    this.boletaSeleccionada = null;
  }

  // === CAMBIO CLAVE: Actualizar estado y forzar UI ===
  cambiarEstado(boleta: BoletaResponse, nuevoEstado: 'ATENDIDA' | 'CANCELADA'): void {
    const accion = nuevoEstado === 'ATENDIDA' ? 'atender' : 'cancelar';
    if (!confirm(`¿Estás seguro de que deseas ${accion} el pedido #${boleta.id}?`)) return;

    this.isLoading = true;

    const serviceCall = nuevoEstado === 'ATENDIDA'
      ? this.boletaService.marcarComoAtendida(boleta.id)
      : this.boletaService.cancelarBoleta(boleta.id);

    serviceCall.subscribe({
      next: (actualizada) => {
        // Actualizar en la lista principal
        const index = this.boletas.findIndex(b => b.id === actualizada.id);
        if (index !== -1) {
          this.boletas[index] = actualizada;
        }

        // Actualizar el modal si está abierto
        if (this.boletaSeleccionada?.id === actualizada.id) {
          this.boletaSeleccionada = actualizada;
        }

        this.isLoading = false;

        // Forzar detección de cambios (clave para que aparezca el botón de PDF)
        this.cdr.detectChanges();

        this.mostrarExito(`Pedido #${actualizada.id} ${nuevoEstado === 'ATENDIDA' ? 'atendido' : 'cancelado'} correctamente.`);
      },
      error: (err) => {
        this.isLoading = false;
        this.mostrarError(`No se pudo ${accion} el pedido. ${err.message || 'Intenta de nuevo.'}`);
      }
    });
  }

  // === DESCARGA DE PDF (sin file-saver) ===
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

        this.mostrarExito(`¡Factura del pedido #${id} descargada correctamente!`);
      },
      error: (err: Error) => {
        this.mostrarError(err.message || 'No se pudo descargar la factura.');
      }
    });
  }

  // Helpers UI
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

  mostrarExito(msg: string) {
    alert(msg);
  }

  mostrarError(msg: string) {
    alert('Error: ' + msg);
  }
}