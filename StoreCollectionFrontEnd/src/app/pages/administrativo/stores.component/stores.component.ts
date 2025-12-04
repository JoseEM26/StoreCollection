// src/app/pages/administrativo/stores/stores.component.ts
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TiendaPublic, TiendaPage } from '../../../model/tienda-public.model';
import { TiendaPublicService } from '../../../service/tienda-public.service';

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
  tiendas = signal<TiendaPublic[]>([]);
  loading = signal(true);

  // Paginación
  currentPage = signal(0);
  pageSize = 12;

  // Modales
  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaPublic | null>(null);

  // Formulario crear
  newStore: Partial<TiendaPublic> = {
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

  constructor(private tiendaService: TiendaPublicService) {}

  ngOnInit(): void {
    this.loadTiendas();
  }

  loadTiendas(page: number = 0) {
    this.loading.set(true);
    this.currentPage.set(page);

    this.tiendaService.getAllTiendas(page, this.pageSize).subscribe({
      next: (pageData) => {
        this.tiendasPage.set(pageData);
        this.tiendas.set(pageData.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        alert('Error al cargar las tiendas');
      }
    });
  }

  // Paginación
  goToPage(page: number) {
    if (page >= 0 && page < (this.tiendasPage()?.totalPages || 0)) {
      this.loadTiendas(page);
    }
  }

  // Abrir modal crear
  openCreateModal() {
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

  closeCreateModal() {
    this.showCreateModal = false;
  }

  createStore() {
    if (!this.newStore.nombre?.trim() || !this.newStore.slug?.trim()) {
      alert('Nombre y slug son obligatorios');
      return;
    }
    alert(`Tienda "${this.newStore.nombre}" creada correctamente (simulado)`);
    this.closeCreateModal();
  }

  // Editar tienda
  openEditModal(tienda: TiendaPublic) {
    this.editingStore.set({ ...tienda });
    this.showEditModal = true;
  }

  closeEditModal() {
    this.showEditModal = false;
    this.editingStore.set(null);
  }

  updateStore() {
    const store = this.editingStore();
    if (store) {
      alert(`Tienda "${store.nombre}" actualizada (simulado)`);
      this.closeEditModal();
    }
  }

  // Toggle activo
  toggleActive(tienda: TiendaPublic) {
    alert(
      tienda.activo
        ? `Tienda "${tienda.nombre}" desactivada (simulado)`
        : `Tienda "${tienda.nombre}" activada (simulado)`
    );
  }
}