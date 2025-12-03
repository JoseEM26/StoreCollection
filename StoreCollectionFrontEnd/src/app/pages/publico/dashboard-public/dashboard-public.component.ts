// src/app/pages/public-dashboard/public-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TiendaPage } from '../../../model/tienda-public.model';
import { TiendaPublicService } from '../../../service/tienda-public.service';

@Component({
  selector: 'app-public-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard-public.component.html',
  styleUrl: './dashboard-public.component.css'
})
export class DashboardPublicComponent implements OnInit {
  page!: TiendaPage;
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
        error: () => this.loading = false
      });
  }

  search(): void {
    this.loadTiendas(0);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.page.totalPages) {
      this.loadTiendas(page);
    }
  }
}