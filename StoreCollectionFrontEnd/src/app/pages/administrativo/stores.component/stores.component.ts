// src/app/pages/admin/stores/stores.component.ts
import { Component, OnInit, OnDestroy, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../../auth/auth.service';
import { FormStoresComponent } from './form-stores/form-stores.component';
import { TiendaAdminPage, TiendaResponse } from '../../../model/admin/tienda-admin.model';
import { TiendaAdminService } from '../../../service/service-admin/tienda-admin.service';
import { SwalService } from '../../../service/SweetAlert/swal.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-stores',
  standalone: true,
  imports: [CommonModule, FormsModule, FormStoresComponent],
  templateUrl: './stores.component.html',
  styleUrl: './stores.component.css'
})
export class StoresComponent implements OnInit, OnDestroy {
  tiendasPage = signal<TiendaAdminPage | null>(null);
  tiendas = signal<TiendaResponse[]>([]);
  loading = signal<boolean>(true);

  showDetailsModal = false;
  selectedTienda: TiendaResponse | null = null;

  currentPage = signal<number>(0);
  pageSize = 12;
  sort = signal<string>('nombre,asc');

  searchInput = signal<string>('');
  searchTerm = signal<string>('');

  showCreateModal = false;
  showEditModal = false;
  editingStore = signal<TiendaResponse | undefined>(undefined);

  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private tiendaService: TiendaAdminService,
    public auth: AuthService,
    private swal: SwalService
  ) {
    // Efecto para recargar cuando cambian los filtros
    effect(() => {
      this.currentPage();
      this.sort();
      this.searchTerm();
      this.loadTiendas();
    }, { allowSignalWrites: true }); // Agregado para permitir escrituras en signals si es necesario

    // Debounce para búsqueda
    effect(() => {
      const term = this.searchInput().trim();
      if (this.debounceTimer) clearTimeout(this.debounceTimer);
      this.debounceTimer = setTimeout(() => {
        if (this.searchTerm() !== term) {
          this.searchTerm.set(term);
          this.currentPage.set(0);
        }
      }, 500);
    });
  }

  ngOnInit(): void {
    this.loadTiendas();
  }

  ngOnDestroy(): void {
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
  }

  // ================================================================
  // RENOVAR TIENDA (Mejorado con chequeo adicional y mensajes más claros)
  // ================================================================
renovarTienda(tienda: TiendaResponse): void {
  const periodo = tienda.planSlug?.includes('year') ? '1 año' : '1 mes';

  Swal.fire({
    title: '¿Renovar el plan de esta tienda?',
    html: `Se extenderá por <strong>${periodo}</strong> a partir de la fecha actual o vencimiento.<br><br>
           <small class="text-muted">Tienda: <strong>${tienda.nombre}</strong><br>
           Vencimiento actual: <strong>${this.formatDate(tienda.fechaVencimiento)}</strong></small>`,  // ← Cambia a fechaVencimiento
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, renovar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#3085d6',
    cancelButtonColor: '#d33',
    allowOutsideClick: false
  }).then((result) => {
    if (result.isConfirmed) {
      this.swal.loading('Renovando plan...');
      this.tiendaService.renovarTienda(tienda.id).subscribe({
        next: (response) => {
          this.swal.close();
          Swal.fire({
            icon: 'success',
            title: '¡Renovado exitosamente!',
            html: `La nueva fecha de vencimiento es: <strong>${this.formatDate(response.fechaVencimiento)}</strong>`, // ← Cambia aquí también
            timer: 3000,
            showConfirmButton: false
          });
          this.loadTiendas();
        },
        error: (err) => {
          this.swal.close();
          Swal.fire({
            icon: 'error',
            title: 'Error al renovar',
            text: err.error?.message || 'No se pudo renovar el plan.',
          });
        }
      });
    }
  });
}

  // ================================================================
  // VISTA PREVIA (Nueva función agregada para visualización previa)
  // ================================================================
  previewTienda(tienda: TiendaResponse): void {
    if (!tienda.activo) {
      this.swal.warning('Tienda inactiva', 'No se puede previsualizar una tienda desactivada. Actívala primero.');
      return;
    }

    const previewUrl = `https://tudominio.com/${tienda.slug}`; // Ajusta a tu URL real de frontend público
    window.open(previewUrl, '_blank');
    this.swal.toast('Abriendo vista previa', 'info');
  }

  // ================================================================
  // CARGA DE DATOS (Mejorado con manejo de errores más detallado)
  // ================================================================
  async loadTiendas(): Promise<void> {
    this.loading.set(true);
    const loadingSwal = this.swal.loading('Cargando tiendas...');

    try {
      const pageData = await this.tiendaService.listarTiendas(
        this.currentPage(),
        this.pageSize,
        this.sort(),
        this.searchTerm().length > 0 ? this.searchTerm() : undefined
      ).toPromise();

      if (pageData) {
        this.tiendasPage.set(pageData);
        this.tiendas.set(pageData.content || []);

        if (pageData.content.length === 0 && this.searchTerm()) {
          this.swal.info('Sin resultados', `No se encontraron tiendas con "${this.searchTerm()}". Intenta con otros términos.`);
        }
      } else {
        this.tiendasPage.set(null);
        this.tiendas.set([]);
      }
    } catch (err: any) {
      console.error('Error cargando tiendas:', err);
      this.swal.error(
        'Error al cargar tiendas',
        err.error?.message || 'No se pudieron cargar las tiendas. Intenta recargar la página o verifica tu conexión.'
      );
      this.tiendasPage.set(null);
      this.tiendas.set([]);
    } finally {
      this.swal.close();
      this.loading.set(false);
    }
  }

  // ================================================================
  // PAGINACIÓN (Sin cambios mayores, ya está bien)
  // ================================================================
  goToPage(page: number): void {
    const totalPages = this.tiendasPage()?.totalPages ?? 0;
    if (page >= 0 && page < totalPages) {
      this.currentPage.set(page);
    }
  }

  getPageNumbers(): number[] {
    const total = this.tiendasPage()?.totalPages ?? 0;
    const current = this.currentPage();
    const delta = 2;
    const range: number[] = [];

    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  // ================================================================
  // MODALES (Mejorado con mensajes de carga si es necesario)
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

  async onFormSuccess(tiendaActualizada: TiendaResponse): Promise<void> {
    this.closeModal();
    await this.loadTiendas();

    const esNueva = !this.editingStore()?.id;
    this.swal.success(
      esNueva ? '¡Tienda creada!' : '¡Tienda actualizada!',
      esNueva 
        ? `La tienda <strong>${tiendaActualizada.nombre}</strong> ya está lista para recibir pedidos. Recuerda asignar un plan si es necesario.`
        : `Los cambios en <strong>${tiendaActualizada.nombre}</strong> se guardaron correctamente. Verifica en la vista previa.`
    );
  }

  // ================================================================
  // ACCIONES (Mejorado con confirmaciones y toasts más consistentes)
  // ================================================================
async toggleActive(tienda: TiendaResponse): Promise<void> {
  const accion = tienda.activo ? 'desactivar' : 'activar';

  const result = await this.swal.confirmAction({
    title: `¿${accion.charAt(0).toUpperCase() + accion.slice(1)} tienda?`,
    text: tienda.activo
      ? `La tienda "${tienda.nombre}" dejará de estar visible públicamente y no procesará pedidos.`
      : `La tienda "${tienda.nombre}" volverá a estar visible y operativa.`,
    icon: tienda.activo ? 'warning' : 'success',
    confirmButtonText: tienda.activo ? 'Sí, desactivar' : 'Sí, activar',
    cancelButtonText: 'Cancelar'
  });

  if (!result?.isConfirmed) return;

  this.swal.loading(`${accion.charAt(0).toUpperCase() + accion.slice(1)}ando tienda...`);

  try {
    const updated = await this.tiendaService.toggleActivo(tienda.id).toPromise();

    if (updated && typeof updated.activo === 'boolean') {
      await this.loadTiendas();
      this.swal.toast(
        updated.activo 
          ? 'Tienda activada correctamente' 
          : 'Tienda desactivada correctamente',
        updated.activo ? 'success' : 'warning'
      );
    }
  } catch (err: any) {
    this.swal.error(
      'Error al cambiar estado',
      err.error?.message || 'No tienes permisos o ocurrió un problema. Intenta nuevamente.'
    );
  } finally {
    this.swal.close();
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

  // ================================================================
  // UTILIDADES VISUALES (Mejorado con más clases de badges y chequeos)
  // ================================================================
  getEstadoSuscripcion(tienda: TiendaResponse | null): string {
    if (!tienda?.estadoSuscripcion) return 'Inactivo';
    const estado = tienda.estadoSuscripcion.toLowerCase();
    return estado === 'trialing' ? 'En prueba' : 
           estado === 'active' ? 'Activo' : 
           estado === 'canceled' ? 'Cancelado' : 
           estado.charAt(0).toUpperCase() + estado.slice(1);
  }

  getPlanBadgeClass(tienda: TiendaResponse): string {
    if (!tienda.planNombre) return 'bg-secondary';

    const plan = tienda.planNombre.toLowerCase();
    const estado = tienda.estadoSuscripcion?.toLowerCase();

    if (estado === 'trialing') return 'bg-warning text-dark';
    if (estado === 'canceled' || estado === 'past_due') return 'bg-danger text-white';
    if (plan.includes('enterprise')) return 'bg-dark text-white';
    if (plan.includes('pro') || plan.includes('premium')) return 'bg-gradient-purple text-white'; // Asumiendo CSS para gradient-purple
    if (plan.includes('básico') || plan.includes('basico')) return 'bg-primary text-white';
    if (plan.includes('gratis') || plan.includes('free')) return 'bg-success text-white';

    return 'bg-info text-white';
  }

  isPlanPremium(tienda: TiendaResponse): boolean {
    if (!tienda.planNombre) return false;
    const plan = tienda.planNombre.toLowerCase();
    return plan.includes('pro') || plan.includes('premium') || plan.includes('enterprise');
  }

  formatDate(isoDate: string | undefined | null): string {
    if (!isoDate) return '—';
    const date = new Date(isoDate);
    return date.toLocaleDateString('es-PE', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).replace('.', '');
  }

  setSort(campo: string): void {
    const [actualCampo, actualDir] = this.sort().split(',');
    const nuevaDir = actualCampo === campo && actualDir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }
}