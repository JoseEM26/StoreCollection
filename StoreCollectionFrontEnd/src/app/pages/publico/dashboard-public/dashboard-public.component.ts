// src/app/pages/public-dashboard/public-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { TiendaPublicService } from '../../../service/tienda-public.service';
import { PlanPublicService } from '../../../service/plan-public.service';
import { TiendaPublicPage } from '../../../model/admin/tienda-admin.model';
import { PlanResponse } from '../../../model/admin/plan-admin.model';

@Component({
  selector: 'app-public-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard-public.component.html',
  styleUrl: './dashboard-public.component.css'
})
export class DashboardPublicComponent implements OnInit {
  page?: TiendaPublicPage;
  searchTerm = '';
  currentPage = 0;
  loading = true;
  loadingPlanes = true;
  planes: PlanResponse[] = [];

  constructor(
    private tiendaService: TiendaPublicService,
    private planService: PlanPublicService
  ) {}

  ngOnInit(): void {
    this.loadTiendas();
    this.loadPlanes();
  }

  // ========================================
  // CARGA DE PLANES PROMOCIONALES
  // ========================================
  loadPlanes(): void {
    this.loadingPlanes = true;
    this.planService.obtenerPlanesPublicos().subscribe({
      next: (planes) => {
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

  // Verifica si un plan promocional está vigente según el mes actual
  private isPlanVigente(mesInicio: number, mesFin: number): boolean {
    const mesActual = new Date().getMonth() + 1; // 1 = enero, 12 = diciembre
    if (mesInicio <= mesFin) {
      return mesActual >= mesInicio && mesActual <= mesFin;
    } else {
      // Cruza fin de año (ej: dic - feb)
      return mesActual >= mesInicio || mesActual <= mesFin;
    }
  }

  getMesesTexto(mesInicio: number, mesFin: number): string {
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
                   'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const inicio = meses[mesInicio - 1] || 'Ene';
    const fin = meses[mesFin - 1] || 'Dic';
    return `${inicio} - ${fin}`;
  }

  // ========================================
  // CARGA DE TIENDAS PÚBLICAS
  // ========================================
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

          // Objeto vacío completo y correcto (con sort en raíz)
          const emptyPage: TiendaPublicPage = {
            content: [],
            pageable: {
              sort: { sorted: false, unsorted: true, empty: true },
              pageNumber: page,
              pageSize: 12,
              offset: 0,
              paged: true,
              unpaged: false
            },
            totalElements: 0,
            totalPages: 0,
            last: true,
            first: true,
            numberOfElements: 0,
            size: 12,
            number: page,
            sort: { sorted: false, unsorted: true, empty: true },
            empty: true
          };

          this.page = emptyPage;
          this.loading = false;
        }
      });
  }

  search(): void {
    this.loadTiendas(0);
  }

  // ========================================
  // PAGINACIÓN MEJORADA
  // ========================================
  getVisiblePages(): number[] {
    if (!this.page || this.page.totalPages <= 1) return [];

    const total = this.page.totalPages;
    const current = this.currentPage;
    const maxVisible = 7;

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