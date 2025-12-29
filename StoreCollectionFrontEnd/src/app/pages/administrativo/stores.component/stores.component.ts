// src/app/pages/admin/stores/stores.component.ts (o la ruta que tengas)
import { Component, OnInit, OnDestroy, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../../auth/auth.service';
import { FormStoresComponent } from './form-stores/form-stores.component';
import { TiendaAdminPage, TiendaResponse } from '../../../model/admin/tienda-admin.model';
import { TiendaAdminService } from '../../../service/service-admin/tienda-admin.service';

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule, FormStoresComponent],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'
})
export class StoresComponent implements OnInit, OnDestroy {
  // Página completa (para paginación)
  tiendasPage = signal<TiendaAdminPage | null>(null);
  
  // Solo el array de tiendas (para *ngFor)
  tiendas = signal<TiendaResponse[]>([]);
  
  loading = signal(true);

  // Modal de detalles
  showDetailsModal = false;
  selectedTienda: TiendaResponse | null = null;

  // Paginación y filtros
  currentPage = signal(0);
  pageSize = 12;
  sort = signal('nombre,asc');
  
  // Búsqueda
  searchInput = signal<string>('');
  searchTerm = signal<string>('');

  // Modales de crear/editar
  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | undefined>(undefined);

  private debounceTimer: any;

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService
  ) {
    // Recarga automática cuando cambian página, orden o término de búsqueda
    effect(() => {
      this.currentPage();
      this.sort();
      this.searchTerm();
      this.loadTiendas();
    });

    // Debounce de 500ms para la búsqueda
    effect(() => {
      const term = this.searchInput().trim();

      clearTimeout(this.debounceTimer);
      this.debounceTimer = setTimeout(() => {
        if (this.searchTerm() !== term) {
          this.searchTerm.set(term);
          this.currentPage.set(0); // Volver a página 1 al buscar
        }
      }, 500);
    });
  }

  ngOnInit(): void {
    this.loadTiendas();
  }

  ngOnDestroy(): void {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }

  // ================================================================
  // CARGA DE DATOS
  // ================================================================
  loadTiendas(): void {
    this.loading.set(true);

    this.tiendaService.listarTiendas(
      this.currentPage(),
      this.pageSize,
      this.sort(),
      this.searchTerm().length > 0 ? this.searchTerm() : undefined
    ).subscribe({
      next: (pageData) => {
        this.tiendasPage.set(pageData);
        this.tiendas.set(pageData.content);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando tiendas:', err);
        this.loading.set(false);
        alert('Error al cargar las tiendas. Revisa la consola para más detalles.');

        // Página vacía en caso de error
        const emptyPage: TiendaAdminPage = {
          content: [],
          pageable: {
            sort: { sorted: false, unsorted: true, empty: true },
            pageNumber: 0,
            pageSize: this.pageSize,
            offset: 0,
            paged: true,
            unpaged: false
          },
          totalElements: 0,
          totalPages: 0,
          last: true,
          first: true,
          numberOfElements: 0,
          size: this.pageSize,
          number: 0,
          sort: { sorted: false, unsorted: true, empty: true },
          empty: true
        };
        this.tiendasPage.set(emptyPage);
        this.tiendas.set([]);
      }
    });
  }

  // ================================================================
  // PAGINACIÓN
  // ================================================================
  goToPage(page: number): void {
    const totalPages = this.tiendasPage()?.totalPages || 0;
    if (page >= 0 && page < totalPages) {
      this.currentPage.set(page);
    }
  }

  getPageNumbers(): number[] {
    const total = this.tiendasPage()?.totalPages || 0;
    const current = this.currentPage();
    const delta = 2;
    const range: number[] = [];

    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  // ================================================================
  // MODALES
  // ================================================================
  openCreateModal(): void {
    this.editingStore.set(undefined);
    this.showCreateModal = true;
  }

  openEditModal(tienda: TiendaResponse): void {
    this.editingStore.set(tienda);
    this.showEditModal = true;
  }

  closeModal(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.editingStore.set(undefined);
  }

  onFormSuccess(tiendaActualizada: TiendaResponse): void {
    this.closeModal();
    this.loadTiendas();
    alert(
      tiendaActualizada.id
        ? `Tienda "${tiendaActualizada.nombre}" actualizada correctamente`
        : `Tienda "${tiendaActualizada.nombre}" creada correctamente`
    );
  }

  // ================================================================
  // ACCIONES
  // ================================================================
  toggleActive(tienda: TiendaResponse): void {
    if (!confirm(
      tienda.activo
        ? `¿Desactivar la tienda "${tienda.nombre}"?`
        : `¿Activar la tienda "${tienda.nombre}"?`
    )) {
      return;
    }

    this.tiendaService.toggleActivo(tienda.id).subscribe({
      next: (updated) => {
        this.loadTiendas();
        alert(updated.activo
          ? `Tienda "${updated.nombre}" activada`
          : `Tienda "${updated.nombre}" desactivada`
        );
      },
      error: (err) => {
        console.error('Error al cambiar estado:', err);
        alert('No tienes permisos o ocurrió un error');
      }
    });
  }

  verDetalles(tienda: TiendaResponse): void {
    this.selectedTienda = tienda;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedTienda = null;
  }

  // ================================================================
  // UTILIDADES VISUALES
  // ================================================================
  getEstadoSuscripcion(tienda: TiendaResponse | null): string {
    if (!tienda || !tienda.estadoSuscripcion) {
      return 'Inactivo';
    }
    const estado = tienda.estadoSuscripcion.toLowerCase();
    return estado.charAt(0).toUpperCase() + estado.slice(1);
  }

  getPlanBadgeClass(tienda: TiendaResponse): string {
    if (!tienda.planNombre) return 'bg-secondary';

    const plan = tienda.planNombre.toLowerCase();
    const estado = tienda.estadoSuscripcion?.toLowerCase();

    if (estado === 'trialing' || estado === 'trial') return 'bg-warning text-dark';
    if (plan.includes('enterprise')) return 'bg-dark';
    if (plan.includes('pro') || plan.includes('premium')) return 'bg-gradient-purple';
    if (plan.includes('básico') || plan.includes('basico')) return 'bg-primary';
    if (plan.includes('gratis') || plan.includes('free')) return 'bg-success';

    return 'bg-info';
  }

  isPlanPremium(tienda: TiendaResponse): boolean {
    if (!tienda.planNombre) return false;
    const plan = tienda.planNombre.toLowerCase();
    return plan.includes('pro') || plan.includes('premium') || plan.includes('enterprise');
  }

  formatDate(isoDate: string | undefined | null): string {
    if (!isoDate) return '-';
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-PE', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    }).replace('.', ''); // Quita el punto que pone Angular en algunos locales
  }

  setSort(campo: string): void {
    const [actualCampo, actualDir] = this.sort().split(',');
    const nuevaDir = actualCampo === campo && actualDir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }
}