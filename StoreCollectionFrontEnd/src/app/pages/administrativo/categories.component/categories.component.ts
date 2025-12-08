import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaPage, CategoriaResponse } from '../../../model/admin/categoria-admin.model';
import { CategoriaAdminService } from '../../../service/service-admin/categoria-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { FormCategoriaComponent } from './form-categoria/form-categoria.component';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, FormCategoriaComponent],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {
  pageData = signal<CategoriaPage | null>(null);
  categorias = signal<CategoriaResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  showCreateModal = false;
  showEditModal = false;
  categoriaEditando = signal<CategoriaResponse | null>(null);

  constructor(
    private categoriaService: CategoriaAdminService,
    public auth: AuthService
  ) {
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
      error: (err) => {
        console.error('Error al cargar categorías:', err);
        this.loading.set(false);
        alert(err.message || 'Error al cargar categorías');
      }
    });
  }

  // Paginación
  goToPage(page: number): void {
    if (page >= 0 && (!this.pageData() || page < this.pageData()!.totalPages)) {
      this.currentPage.set(page);
    }
  }

  onSearch(): void {
    this.currentPage.set(0);
  }

  toggleSort(): void {
    const current = this.sort();
    this.sort.set(current.includes('asc') ? 'nombre,desc' : 'nombre,asc');
    this.currentPage.set(0);
  }

  // MODALES
  openCreateModal(): void {
    this.categoriaEditando.set(null);
    this.showCreateModal = true;
  }

  openEditModal(categoria: CategoriaResponse): void {
    this.categoriaEditando.set(categoria);
    this.showEditModal = true;
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.categoriaEditando.set(null);
  }

  // ÉXITO AL CREAR O EDITAR
  onCategoriaGuardada(categoria: CategoriaResponse) {
    alert(
      categoria.id
        ? `Categoría "${categoria.nombre}" actualizada correctamente`
        : `Categoría "${categoria.nombre}" creada exitosamente`
    );
    this.closeModals();
    this.loadCategorias();
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