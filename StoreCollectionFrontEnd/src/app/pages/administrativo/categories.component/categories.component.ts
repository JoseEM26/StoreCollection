// src/app/pages/administrativo/categories/categories.component.ts

import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CategoriaPage, CategoriaResponse } from '../../../model/admin/categoria-admin.model';
import { CategoriaAdminService } from '../../../service/service-admin/categoria-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { CategoryFormComponent } from './category-form/category-form.component';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, CategoryFormComponent],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {
  // Datos
  pageData = signal<CategoriaPage | null>(null);
  categorias = signal<CategoriaResponse[]>([]);
  loading = signal(true);

  // Modal
  showModal = false;
  isEditMode = false;
  selectedCategoria?: CategoriaResponse;

  // Filtros y paginación
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  constructor(
    private categoriaService: CategoriaAdminService,
    public auth: AuthService
  ) {
    // Recargar automáticamente al cambiar página, orden o búsqueda
    effect(() => {
      this.loadCategorias();
    });
  }

  ngOnInit(): void {
    this.loadCategorias();
  }

  loadCategorias(): void {
    this.loading.set(true);

    this.categoriaService.listarCategorias(
      this.currentPage(),
      this.pageSize,
      this.sort(),
      this.searchTerm.trim() || undefined
    ).subscribe({
      next: (data) => {
        this.pageData.set(data);
        this.categorias.set(data.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        alert('Error al cargar las categorías');
      }
    });
  }

  // === Modal ===
  openCreateModal(): void {
    this.isEditMode = false;
    this.selectedCategoria = undefined;
    this.showModal = true;
  }

  openEditModal(categoria: CategoriaResponse): void {
    this.isEditMode = true;
    this.selectedCategoria = categoria;
    this.showModal = true;
  }
// Agrega estos getters al final de tu clase CategoriesComponent

get startItem(): number {
  return this.currentPage() * this.pageSize + 1;
}

get endItem(): number {
  const end = (this.currentPage() + 1) * this.pageSize;
  return Math.min(end, this.pageData()?.totalElements || 0);
}
  closeModal(): void {
    this.showModal = false;
  }

  onCategorySaved(): void {
    this.closeModal();
    this.loadCategorias(); // Recargar lista tras crear/editar
  }

  // === Toggle Activo (solo ADMIN) ===
  toggleActivo(categoria: CategoriaResponse): void {

    this.categoriaService.toggleActivo(categoria.id).subscribe({
      next: (updated) => {
        this.categorias.update(cats =>
          cats.map(c => c.id === updated.id ? updated : c)
        );
      },
      error: () => alert('Error al cambiar el estado de la categoría')
    });
  }

  // === Paginación ===
  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
      this.currentPage.set(page);
    }
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

  // === Búsqueda y orden ===
  onSearch(): void {
    this.currentPage.set(0);
  }

  toggleSort(): void {
    const current = this.sort();
    this.sort.set(current.includes('asc') ? 'nombre,desc' : 'nombre,asc');
    this.currentPage.set(0);
  }
}