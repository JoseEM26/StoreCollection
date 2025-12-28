import { Component, OnInit, signal, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TiendaPage } from '../../../model/tienda-public.model';
import { TiendaResponse } from '../../../model/admin/tienda-admin.model';
import { TiendaAdminService } from '../../../service/service-admin/tienda-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { FormStoresComponent } from './form-stores/form-stores.component';

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule, FormStoresComponent],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'
})
export class StoresComponent implements OnInit, OnDestroy {
  tiendasPage = signal<TiendaPage | null>(null);
  tiendas = signal<TiendaResponse[]>([]);
  loading = signal(true);
// Para el modal de detalles
showDetailsModal = false;
selectedTienda: TiendaResponse | null = null;
  currentPage = signal(0);
  pageSize = 12;
  sort = signal('nombre,asc');
searchInputValue = '';
  // Búsqueda con debounce
  searchInput = signal<string>('');
  searchTerm = signal<string>('');

  // Modales
  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | undefined>(undefined);

  private debounceTimer: any;

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService
  ) {
    // Recarga automática cuando cambian página, orden o búsqueda
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
          this.currentPage.set(0);
        }
      }, 500);
    });
  }
getEstadoSuscripcion(tienda: TiendaResponse | null): string {
  if (!tienda || !tienda.estadoSuscripcion) {
    return 'Inactivo';
  }
  return tienda.estadoSuscripcion.charAt(0).toUpperCase() + tienda.estadoSuscripcion.slice(1).toLowerCase();
}
  ngOnInit(): void {
    this.loadTiendas();
  }

  ngOnDestroy(): void {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }


verDetalles(tienda: TiendaResponse): void {
  this.selectedTienda = tienda;
  this.showDetailsModal = true;
}

closeDetailsModal(): void {
  this.showDetailsModal = false;
  this.selectedTienda = null;
}

// Ya tienes estas funciones (formatDate e isPlanPremium) del mensaje anterior
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
        this.tiendas.set(pageData.content as TiendaResponse[]);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error cargando tiendas:', err);
        this.loading.set(false);
        alert('Error al cargar las tiendas');
      }
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && (!this.tiendasPage() || page < this.tiendasPage()!.totalPages)) {
      this.currentPage.set(page);
    }
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
        ? `Tienda "${tiendaActualizada.nombre}" actualizada`
        : `Tienda "${tiendaActualizada.nombre}" creada`
    );
  }

  setSort(campo: string): void {
    const [actualCampo, actualDir] = this.sort().split(',');
    const nuevaDir = actualCampo === campo && actualDir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
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

  openCreateModal(): void {
    this.editingStore.set(undefined);
    this.showCreateModal = true;
  }

  openEditModal(tienda: TiendaResponse): void {
    this.editingStore.set(tienda);
    this.showEditModal = true;
  }

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
        alert(
          updated.activo
            ? `Tienda "${updated.nombre}" activada correctamente`
            : `Tienda "${updated.nombre}" desactivada correctamente`
        );
      },
      error: (err) => {
        console.error('Error al toggle activo:', err);
        alert('No tienes permisos para realizar esta acción o ocurrió un error');
      }
    });
  }

  // ================================================================
  // MÉTODOS PARA EL NUEVO DISEÑO DEL PLAN
  // ================================================================

  /**
   * Devuelve la clase CSS del badge según el plan y estado
   */
  getPlanBadgeClass(tienda: TiendaResponse): string {
    if (!tienda.planNombre) return 'bg-secondary';

    const plan = tienda.planNombre.toLowerCase();
    const estado = tienda.estadoSuscripcion;

    // Trial siempre amarillo
    if (estado === 'trial') return 'bg-warning text-dark';

    // Planes premium
    if (plan.includes('enterprise')) return 'bg-dark';
    if (plan.includes('premium') || plan.includes('pro')) return 'bg-gradient-purple';

    // Planes estándar
    if (plan.includes('básico') || plan.includes('basico')) return 'bg-primary';
    if (plan.includes('gratis')) return 'bg-success';

    // Por defecto
    return 'bg-info';
  }

  /**
   * Detecta si es un plan premium para mostrar estrella o icono especial
   */
  isPlanPremium(tienda: TiendaResponse): boolean {
    if (!tienda.planNombre) return false;
    const plan = tienda.planNombre.toLowerCase();
    return plan.includes('pro') || plan.includes('premium') || plan.includes('enterprise');
  }

  /**
   * Formatea fecha ISO (2025-12-27T...) a formato legible en español
   * Ejemplo: "27 dic 2025"
   */
  formatDate(isoDate: string | undefined): string {
    if (!isoDate) return '';
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-PE', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }
}