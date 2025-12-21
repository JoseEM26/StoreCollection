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
    this.planService.listar(this.currentPage(), this.pageSize, this.searchTerm)
      .subscribe({
        next: (data) => {
          this.pageData.set(data);
          this.planes.set(data.content);
          this.loading.set(false);
        },
        error: (err) => {
          console.error(err);
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
    this.loadPlanes();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
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
    const current = this.currentPage();
    const delta = 2;
    const range = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  trackById(index: number, plan: PlanResponse): number {
    return plan.id;
  }

  formatPrecio(precio: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(precio);
  }

  toggleActivo(plan: PlanResponse): void {
    this.planService.toggleActivo(plan.id).subscribe({
      next: (updatedPlan) => {
        this.planes.update(list =>
          list.map(p => p.id === updatedPlan.id ? updatedPlan : p)
        );
      },
      error: () => {
        alert('Error al cambiar el estado del plan');
      }
    });
  }

  getMesesTexto(mesInicio: number, mesFin: number): string {
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${meses[mesInicio - 1]} - ${meses[mesFin - 1]}`;
  }
}