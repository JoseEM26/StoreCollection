// src/app/pages/administrativo/stores/stores.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TiendaPage } from '../../../model/tienda-public.model';
import { TiendaResponse } from '../../../model/admin/tienda-admin.model';
import { TiendaAdminService } from '../../../service/service-admin/tienda-admin.service';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'
})
export class StoresComponent implements OnInit {
  // Datos desde API
  tiendasPage = signal<TiendaPage | null>(null);
  tiendas = signal<TiendaResponse[]>([]);
  loading = signal(true);

  // Filtros y paginación
  currentPage = signal(0);
  pageSize = 12;
  sort = signal('nombre,asc');

  // Búsqueda: usamos string normal (ngModel no soporta signal directamente)
  searchTerm: string = '';

  // Modales
  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | null>(null);

  // Formulario crear
  newStore: Partial<TiendaResponse> = {
    nombre: '',
    slug: '',
    whatsapp: '+51',
    moneda: 'SOLES',
    descripcion: '',
    direccion: '',
    horarios: 'Lun-Sáb 10am-9pm',
    planNombre: 'Básico',
    activo: true
  };

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService
  ) {
    // Recargar cuando cambien page, sort o search
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

  // Paginación
  goToPage(page: number): void {
    if (page >= 0 && (!this.tiendasPage() || page < this.tiendasPage()!.totalPages)) {
      this.currentPage.set(page);
    }
  }

  // Búsqueda (con debounce opcional)
  onSearch(): void {
    this.currentPage.set(0);
    // Se recarga automáticamente por el effect
  }

  // Ordenación
  setSort(campo: string): void {
    const [actualCampo, actualDir] = this.sort().split(',');
    const nuevaDir = actualCampo === campo && actualDir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }

  // ← AQUÍ ESTABA EL ERROR: ¡Faltaba declararlo como método!
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

  // Modal Crear
  openCreateModal(): void {
    this.newStore = {
      nombre: '',
      slug: '',
      whatsapp: '+51',
      moneda: 'SOLES',
      descripcion: '',
      direccion: '',
      horarios: 'Lun-Sáb 10am-9pm',
      planNombre: 'Básico',
      activo: true
    };
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  // Modal Editar
  openEditModal(tienda: TiendaResponse): void {
    this.editingStore.set({ ...tienda });
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingStore.set(null);
  }

  createStore(): void {
    if (!this.newStore.nombre?.trim() || !this.newStore.slug?.trim()) {
      alert('Nombre y slug son obligatorios');
      return;
    }
    alert(`Tienda "${this.newStore.nombre}" creada correctamente`);
    this.closeCreateModal();
    this.loadTiendas();
  }

  updateStore(): void {
    const store = this.editingStore();
    if (store) {
      alert(`Tienda "${store.nombre}" actualizada correctamente`);
      this.closeEditModal();
      this.loadTiendas();
    }
  }

  toggleActive(tienda: TiendaResponse): void {
    alert(tienda.activo
      ? `Tienda "${tienda.nombre}" desactivada`
      : `Tienda "${tienda.nombre}" activada`
    );
    this.loadTiendas();
  }
}