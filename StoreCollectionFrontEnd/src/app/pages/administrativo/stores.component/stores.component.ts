import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TiendaPage } from '../../../model/tienda-public.model';
import { TiendaResponse } from '../../../model/admin/tienda-admin.model';
import { TiendaAdminService } from '../../../service/service-admin/tienda-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { FormStoresComponent } from "./form-stores/form-stores.component";

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule, FormStoresComponent],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'  // ← Apunta al archivo .css
})
export class StoresComponent implements OnInit {
  tiendasPage = signal<TiendaPage | null>(null);
  tiendas = signal<TiendaResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 12;
  sort = signal('nombre,asc');
  searchTerm: string = '';

  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | undefined>(undefined);

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService
  ) {
    effect(() => {
      this.loadTiendas();
    });
  }

  ngOnInit(): void {
    this.loadTiendas();
  }

  loadTiendas(): void {
    this.loading.set(true);
    this.tiendaService.listarTiendas(
      this.currentPage(),
      this.pageSize,
      this.sort(),
      this.searchTerm.trim() || undefined
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
closeModal() {
  this.showCreateModal = false;
  this.showEditModal = false;
  this.editingStore.set(undefined);
}
  onSearch(): void {
    this.currentPage.set(0);
  }

  onFormSuccess(tiendaActualizada: TiendaResponse) {
    this.closeCreateModal();
    this.closeEditModal();
    this.loadTiendas();
    alert(tiendaActualizada.id ? `Tienda "${tiendaActualizada.nombre}" actualizada` : `Tienda "${tiendaActualizada.nombre}" creada`);
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
    alert(tienda.activo ? `Tienda "${tienda.nombre}" desactivada` : `Tienda "${tienda.nombre}" activada`);
    this.loadTiendas();
  }
}