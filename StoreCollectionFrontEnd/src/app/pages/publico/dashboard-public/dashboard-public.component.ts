// src/app/pages/public-dashboard/public-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TiendaPage, TiendaPublic } from '../../../model/tienda-public.model';
import { TiendaPublicService } from '../../../service/tienda-public.service';

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

  constructor(private tiendaService: TiendaPublicService) {}

  ngOnInit(): void {
    this.loadTiendas();
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
  goToPage(page: number): void {
    if (this.page && page >= 0 && page < this.page.totalPages) {
      this.loadTiendas(page);
    }
  }
}