// src/app/pages/public-dashboard/public-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TiendaPage, TiendaPublic } from '../../../model/tienda-public.model';
import { TiendaPublicService } from '../../../service/tienda-public.service';
import { PlanPublicService } from '../../../service/plan-public.service';  // ← NUEVO
import { PlanResponse } from '../../../model/admin/plan-admin.model';

@Component({
  selector: 'app-public-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard-public.component.html',
  styleUrl: './dashboard-public.component.css'
})
export class DashboardPublicComponent implements OnInit {
  page?: TiendaPage;
  searchTerm = '';
  currentPage = 0;
  loading = true;
  loadingPlanes = true;
  planes: PlanResponse[] = [];  // ← NUEVO: planes dinámicos

  constructor(
    private tiendaService: TiendaPublicService,
    private planService: PlanPublicService  // ← NUEVO
  ) {}

  ngOnInit(): void {
    this.loadTiendas();
    this.loadPlanes();  // ← NUEVO
  }
// Dentro de src/app/pages/public-dashboard/public-dashboard.component.ts

getMesesTexto(mesInicio: number, mesFin: number): string {
  const meses = [
    'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
    'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'
  ];

  // Validación por si vienen valores fuera de rango (seguridad extra)
  const inicio = meses[mesInicio - 1] || 'Ene';
  const fin = meses[mesFin - 1] || 'Dic';

  return `${inicio} - ${fin}`;
}
loadPlanes(): void {
    this.loadingPlanes = true;
    this.planService.obtenerPlanesPublicos().subscribe({
      next: (planes) => {
        // El backend ya filtra por activo=true y esVisiblePublico=true
        // Además están ordenados por el campo 'orden'
        this.planes = planes;
        this.loadingPlanes = false;
      },
      error: (err) => {
        console.error('Error al cargar planes públicos', err);
        this.planes = [];
        this.loadingPlanes = false;
      }
    });
  }

  // NUEVO: Verificar vigencia basado en mes actual (diciembre = 12)
  private isPlanVigente(mesInicio: number, mesFin: number): boolean {
    const mesActual = new Date().getMonth() + 1;  // 1-12
    if (mesInicio <= mesFin) {
      return mesActual >= mesInicio && mesActual <= mesFin;
    } else {
      // Rango que cruza fin de año
      return mesActual >= mesInicio || mesActual <= mesFin;
    }
  }

  loadTiendas(page = 0): void {
    this.loading = true;
    this.currentPage = page;

    this.tiendaService.getAllTiendas(page, 12, 'nombre,asc', this.searchTerm)
      .subscribe({
        next: (data) => {
          this.page = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error al cargar tiendas públicas', err);
          this.page = {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            numberOfElements: 0,
            first: true,
            last: true,
            size: 12,
            pageable: { pageNumber: 0, pageSize: 12, sort: { sorted: true, unsorted: false, empty: false } }
          } as TiendaPage;
          this.loading = false;
        }
      });
  }

  search(): void {
    this.loadTiendas(0);
  }

  getPageRange(): number[] {
    if (!this.page) return [];
    const total = this.page.totalPages;
    const current = this.currentPage;
    const range = [];

    const start = Math.max(0, current - 2);
    const end = Math.min(total, current + 3);

    for (let i = start; i < end; i++) {
      range.push(i);
    }
    return range;
  }
// En DashboardPublicComponent (dentro del .ts)

getVisiblePages(): number[] {
  if (!this.page || this.page.totalPages <= 1) return [];

  const total = this.page.totalPages;
  const current = this.currentPage;
  const maxVisible = 7; // Máximo 7 botones visibles

  let start = Math.max(0, current - Math.floor(maxVisible / 2));
  let end = start + maxVisible;

  if (end > total) {
    end = total;
    start = Math.max(0, end - maxVisible);
  }

  return Array.from({ length: end - start }, (_, i) => start + i);
}
  goToPage(page: number): void {
    if (this.page && page >= 0 && page < this.page.totalPages) {
      this.loadTiendas(page);
    }
  }
}