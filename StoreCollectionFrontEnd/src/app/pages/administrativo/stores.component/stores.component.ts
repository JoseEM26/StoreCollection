import { Component, OnInit, signal, effect } from '@angular/core';
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
export class StoresComponent implements OnInit {
  tiendasPage = signal<TiendaPage | null>(null);
  tiendas = signal<TiendaResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 12;
  sort = signal('nombre,asc');

  // Signal para lo que escribe el usuario (actualiza al instante en el input)
  searchInput = signal<string>('');

  // Signal para el término que se envía al backend (con debounce)
  searchTerm = signal<string>('');

  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | undefined>(undefined);

  private debounceTimer: any;

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService
  ) {
    // Effect principal: recarga cuando cambie página, orden o término de búsqueda (debounced)
    effect(() => {
      this.currentPage();
      this.sort();
      this.searchTerm();
      this.loadTiendas();
    });

    // Effect para debounce: actualiza searchTerm 500ms después de que el usuario deje de escribir
    effect(() => {
      const term = this.searchInput().trim();

      clearTimeout(this.debounceTimer);
      this.debounceTimer = setTimeout(() => {
        if (this.searchTerm() !== term) {
          this.searchTerm.set(term);
          this.currentPage.set(0); // Reinicia paginación al buscar
        }
      }, 500);
    });
  }

  ngOnInit(): void {
    this.loadTiendas();
  }

  ngOnDestroy(): void {
    // Limpia el timer si el componente se destruye
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }

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

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  openEditModal(tienda: TiendaResponse): void {
    this.editingStore.set(tienda);
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingStore.set(undefined);
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
}