// src/app/pages/administrativo/categories/categories.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaPage, CategoriaResponse } from '../../../model/admin/categoria-admin.model';
import { CategoriaAdminService } from '../../../service/service-admin/categoria-admin.service';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {
  // Datos del backend
  pageData = signal<CategoriaPage | null>(null);
  categorias = signal<CategoriaResponse[]>([]);
  loading = signal(true);

  // Filtros
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  // Modal crear
  showCreateModal = false;
  newCategory = { nombre: '' };

  constructor(
    private categoriaService: CategoriaAdminService,
    public auth: AuthService
  ) {
    // Recarga automática cuando cambien filtros
    effect(() => this.loadCategorias());
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
        alert('Error al cargar categorías');
      }
    });
  }

  // Paginación
  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
      this.currentPage.set(page);
    }
  }

  // Búsqueda
  onSearch(): void {
    this.currentPage.set(0);
  }

  // Ordenación
  toggleSort(): void {
    const current = this.sort();
    this.sort.set(current.includes('asc') ? 'nombre,desc' : 'nombre,asc');
    this.currentPage.set(0);
  }

  // Modal
  openCreateModal(): void {
    this.newCategory.nombre = '';
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  saveCategory(): void {
    if (!this.newCategory.nombre.trim()) {
      alert('El nombre es obligatorio');
      return;
    }

    // Aquí iría el POST real al backend
    alert(`Categoría "${this.newCategory.nombre}" creada correctamente (simulado)`);

    this.closeCreateModal();
    this.loadCategorias(); // recargar lista
  }

  // Paginación inteligente
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
}