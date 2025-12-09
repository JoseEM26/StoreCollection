// src/app/pages/administrativo/products/products.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPage, ProductoResponse } from '../../../model/admin/producto-admin.model';
import { ProductoAdminService } from '../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { ProductFormComponent } from './product-form/product-form.component';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductFormComponent],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent {
  pageData = signal<ProductoPage | null>(null);
  productos = signal<ProductoResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  // Formulario avanzado
  showForm = signal(false);
  editingId = signal<number | null>(null);

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    this.loadProductos();
  }

  loadProductos(): void {
    this.loading.set(true);
    this.productoService.listarProductos(
      this.currentPage(),
      this.pageSize,
      this.sort(),
      this.searchTerm.trim() || undefined
    ).subscribe({
      next: (data) => {
        this.pageData.set(data);
        this.productos.set(data.content);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        alert('Error al cargar productos');
      }
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
      this.currentPage.set(page);
      this.loadProductos();
    }
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadProductos();
  }

  setSort(campo: string): void {
    const [actual, dir] = this.sort().split(',');
    const nuevaDir = actual === campo && dir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
    this.loadProductos();
  }

  openCreateModal(): void {
    this.editingId.set(null);
    this.showForm.set(true);
  }

  openEditModal(producto: ProductoResponse): void {
    this.editingId.set(producto.id);
    this.showForm.set(true);
  }

  getPageNumbers(): number[] {
    const total = this.pageData()?.totalPages || 0;
    const current = this.currentPage();
    const delta = 2;
    const range = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    if (!range.includes(0)) range.unshift(0);
    if (!range.includes(total - 1) && total > 1) range.push(total - 1);
    return range;
  }
}