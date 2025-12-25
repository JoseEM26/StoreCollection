import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BoletaService, BoletaPageResponse } from '../../../service/boleta.service';
import { BoletaResponse, BoletaDetalleResponse } from '../../../model/boleta.model';

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

  // Paginación
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  // Filtros
  selectedEstado: '' | 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA' = '';
  tiendaId: number = 1; // TODO: Obtener dinámicamente desde auth/service en producción

  isLoading = false;
  errorMessage: string | null = null;

  constructor(private boletaService: BoletaService) {}

  ngOnInit(): void {
    this.cargarBoletas();
  }

  /** Carga las boletas con paginación y filtros actuales */
  cargarBoletas(page: number = this.currentPage): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.boletaService.getBoletasPaginadas(
      page,
      this.pageSize,
      'fecha,desc',
      this.selectedEstado || undefined, // '' → undefined → sin filtro
      this.tiendaId
    ).subscribe({
      next: (pageData: BoletaPageResponse) => {
        this.boletas = pageData.content ?? [];
        this.currentPage = pageData.number;
        this.totalPages = pageData.totalPages;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'No pudimos cargar las boletas. Intenta de nuevo.';
        this.isLoading = false;
        console.error('Error cargando boletas:', err);
      }
    });
  }

  // Navegación de páginas
  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.cargarBoletas(this.currentPage - 1);
    }
  }

  paginaSiguiente(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.cargarBoletas(this.currentPage + 1);
    }
  }

  // Rangos visibles para paginador (evita cálculos complejos en template)
  get visiblePages(): number[] {
    const range = 2; // muestra 2 páginas a cada lado del actual
    const start = Math.max(0, this.currentPage - range);
    const end = Math.min(this.totalPages - 1, this.currentPage + range);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  // Ver detalle de boleta
  verDetalle(boleta: BoletaResponse): void {
    this.boletaSeleccionada = boleta;
  }

  cerrarDetalle(): void {
    this.boletaSeleccionada = null;
  }

  // Cambiar estado de boleta
  cambiarEstado(boleta: BoletaResponse, nuevoEstado: 'ATENDIDA' | 'CANCELADA'): void {
    const accion = nuevoEstado === 'ATENDIDA' ? 'atender' : 'cancelar';

    if (!confirm(`¿Realmente deseas ${accion} la boleta #${boleta.id}?`)) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    this.boletaService.actualizarEstado(boleta.id, nuevoEstado).subscribe({
      next: (boletaActualizada) => {
        // Actualiza en la lista principal
        const index = this.boletas.findIndex(b => b.id === boletaActualizada.id);
        if (index !== -1) {
          this.boletas[index] = boletaActualizada;
        }

        // Actualiza detalle si está abierto
        if (this.boletaSeleccionada?.id === boletaActualizada.id) {
          this.boletaSeleccionada = boletaActualizada;
        }

        this.isLoading = false;
        alert(`La boleta #${boletaActualizada.id} ha sido ${nuevoEstado.toLowerCase()} exitosamente.`);
      },
      error: (err) => {
        console.error(`Error al ${accion} boleta #${boleta.id}:`, err);
        this.errorMessage = `No se pudo ${accion} la boleta #${boleta.id}. Intenta nuevamente.`;
        this.isLoading = false;
      }
    });
  }

  filtrarPorEstado(): void {
    this.currentPage = 0; // Reset a primera página
    this.cargarBoletas();
  }

  // Helpers para template
  getEstadoBadge(estado: string): { text: string; class: string } {
    const map: Record<string, { text: string; class: string }> = {
      'PENDIENTE': { text: 'Pendiente', class: 'bg-warning text-dark' },
      'ATENDIDA':  { text: 'Atendida',  class: 'bg-success text-white' },
      'CANCELADA': { text: 'Cancelada', class: 'bg-danger text-white' }
    };
    return map[estado] || { text: estado, class: 'bg-secondary text-white' };
  }

  formatDate(fecha: string): string {
    return new Date(fecha).toLocaleString('es-PE', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getTotalItems(detalles: BoletaDetalleResponse[] | undefined): number {
    if (!detalles) return 0;
    return detalles.reduce((sum, item) => sum + (item.cantidad || 0), 0);
  }
}