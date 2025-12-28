// src/app/pages/administrativo/planes/planes.component.ts
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlanAdminService } from '../../../service/service-admin/plan-admin.service';
import { PlanPage, PlanResponse } from '../../../model/admin/plan-admin.model';
import { PlanesFormComponent } from './planes-form/planes-form.component';

@Component({
  selector: 'app-planes',
  standalone: true,
  imports: [CommonModule, FormsModule, PlanesFormComponent],
  templateUrl: './planes.component.html',
  styleUrl: './planes.component.css'
})
export class PlanesComponent implements OnInit {
  pageData = signal<PlanPage | null>(null);
  planes = signal<PlanResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 10;
  searchTerm = '';

  showModal = signal(false);
  editingPlan = signal<PlanResponse | null>(null);

  private searchTimeout: any;

  constructor(private planService: PlanAdminService) {}

  ngOnInit(): void {
    this.loadPlanes();
  }

  loadPlanes(): void {
    this.loading.set(true);
    
    // Usamos el sort por defecto del backend: id,desc
    const sort = ['id,desc'];
    
    this.planService.listar(this.currentPage(), this.pageSize, sort)
      .subscribe({
        next: (data) => {
          this.pageData.set(data);
          this.planes.set(data.content);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error al cargar los planes', err);
          this.planes.set([]);
          this.pageData.set(null);
          this.loading.set(false);
          alert('Error al cargar los planes');
        }
      });
  }

  abrirCrear(): void {
    this.editingPlan.set(null);
    this.showModal.set(true);
  }

  abrirEditar(plan: PlanResponse): void {
    this.editingPlan.set(plan);
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingPlan.set(null);
  }

  onFormSuccess(): void {
    this.closeModal();
    this.loadPlanes();  // Recargar lista después de crear/editar
  }

  goToPage(page: number): void {
    if (page >= 0 && this.pageData() && page < this.pageData()!.totalPages) {
      this.currentPage.set(page);
      this.loadPlanes();
    }
  }

  onSearch(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage.set(0);
      this.loadPlanes();
    }, 500);
  }

  get startItem(): number {
    return this.currentPage() * this.pageSize + 1;
  }

  get endItem(): number {
    const calculated = (this.currentPage() + 1) * this.pageSize;
    const total = this.pageData()?.totalElements || 0;
    return calculated > total ? total : calculated;
  }

  getPageNumbers(): number[] {
    const total = this.pageData()?.totalPages || 0;
    if (total <= 1) return [];

    const current = this.currentPage();
    const delta = 2;
    const range = [];
    const start = Math.max(0, current - delta);
    const end = Math.min(total - 1, current + delta);

    for (let i = start; i <= end; i++) {
      range.push(i);
    }
    return range;
  }

  trackById(index: number, plan: PlanResponse): number {
    return plan.id;
  }

  // Formatear precio en soles peruanos
  formatPrecio(precio: number | null | undefined): string {
    if (precio == null) return 'Gratis';
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 0
    }).format(precio);
  }

  // Mostrar precio mensual y anual si existe
  getPrecioTexto(plan: PlanResponse): string {
    const mensual = this.formatPrecio(plan.precioMensual);
    if (plan.precioAnual) {
      const anual = this.formatPrecio(plan.precioAnual);
      return `${mensual}/mes | ${anual}/año`;
    }
    return mensual + '/mes';
  }
calcularAhorro(plan: PlanResponse): number {
  if (!plan.precioAnual || plan.precioMensual === 0) return 0;
  const anualEquivalente = plan.precioMensual * 12;
  const ahorro = ((anualEquivalente - plan.precioAnual) / anualEquivalente) * 100;
  return Math.round(ahorro);
}
  toggleActivo(plan: PlanResponse): void {
    this.planService.toggleActivo(plan.id).subscribe({
      next: (updatedPlan) => {
        this.planes.update(list =>
          list.map(p => p.id === updatedPlan.id ? updatedPlan : p)
        );
      },
      error: (err) => {
        console.error('Error al toggle activo', err);
        alert('Error al cambiar el estado del plan');
      }
    });
  }
}