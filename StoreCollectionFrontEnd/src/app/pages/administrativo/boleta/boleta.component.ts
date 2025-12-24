import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BoletaService } from '../../../service/boleta.service';
import { BoletaResponse, BoletaPageResponse, BoletaDetalleResponse } from '../../../model/boleta.model';

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
  selectedEstado: '' | 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA' = ''; // Literal union + ''
  tiendaId: number = 1; // ← Cambiar por auth en producción

  isLoading = false;
  errorMessage: string | null = null;

  constructor(private boletaService: BoletaService) {}

  ngOnInit(): void {
    this.cargarBoletas();
  }

cargarBoletas(page: number = this.currentPage) {
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
      this.boletas = pageData.content;
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
  paginaAnterior() {
    if (this.currentPage > 0) {
      this.cargarBoletas(this.currentPage - 1);
    }
  }

  paginaSiguiente() {
    if (this.currentPage < this.totalPages - 1) {
      this.cargarBoletas(this.currentPage + 1);
    }
  }

  // Rangos visibles para paginador (evita Math en template)
  get visiblePages(): number[] {
    const range = 3; // muestra 3 páginas a cada lado
    const start = Math.max(0, this.currentPage - range);
    const end = Math.min(this.totalPages - 1, this.currentPage + range);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  verDetalle(boleta: BoletaResponse) {
    this.boletaSeleccionada = boleta;
  }

  cerrarDetalle() {
    this.boletaSeleccionada = null;
  }

  cambiarEstado(boleta: BoletaResponse, nuevoEstado: 'ATENDIDA' | 'CANCELADA') {
    const accion = nuevoEstado === 'ATENDIDA' ? 'atender' : 'cancelar';

    if (confirm(`¿Estás seguro de querer ${accion} la boleta #${boleta.id}?`)) {
      this.isLoading = true;

      this.boletaService.actualizarEstado(boleta.id, nuevoEstado).subscribe({
        next: (actualizada) => {
          const index = this.boletas.findIndex(b => b.id === actualizada.id);
          if (index !== -1) this.boletas[index] = actualizada;

          if (this.boletaSeleccionada?.id === actualizada.id) {
            this.boletaSeleccionada = actualizada;
          }

          this.isLoading = false;
        },
        error: (err) => {
          this.errorMessage = `No se pudo ${accion} la boleta.`;
          this.isLoading = false;
          console.error(err);
        }
      });
    }
  }

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
filtrarPorEstado() {
  this.currentPage = 0; // Resetear a la primera página al cambiar filtro
  this.cargarBoletas();
}
  getTotalItems(detalles: BoletaDetalleResponse[]): number {
    return detalles.reduce((sum, item) => sum + item.cantidad, 0);
  }
}