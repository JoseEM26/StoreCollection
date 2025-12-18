// src/app/pages/administrativo/products/products.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
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
export class ProductsComponent implements OnInit {
  // Datos
  pageData = signal<ProductoPage | null>(null);
  productos = signal<ProductoResponse[]>([]);
  loading = signal(true);

  // Modal
  showModal = false;
  isEditMode = false;
  selectedProducto?: ProductoResponse;

  // Filtros y paginación
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    // Recargar automáticamente al cambiar filtros
    effect(() => {
      this.loadProductos();
    });
  }

  ngOnInit(): void {
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
        alert('Error al cargar los productos');
      }
    });
  }

  // === Modal ===
  openCreateModal(): void {
    this.isEditMode = false;
    this.selectedProducto = undefined;
    this.showModal = true;
  }

  openEditModal(producto: ProductoResponse): void {
    this.isEditMode = true;
    this.selectedProducto = producto;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  onProductSaved(): void {
    this.closeModal();
    this.loadProductos(); // Recargar lista
  }

  // === Toggle Activo (solo ADMIN) ===
  toggleActivo(producto: ProductoResponse): void {
    if (!this.auth.isAdmin()) {
      alert('Solo los administradores pueden cambiar el estado');
      return;
    }

    this.productoService.toggleActivo(producto.id).subscribe({
      next: (updated) => {
        this.productos.update(prods =>
          prods.map(p => p.id === updated.id ? updated : p)
        );
      },
      error: () => alert('Error al cambiar el estado del producto')
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

  setSort(campo: string): void {
    const [actual, dir] = this.sort().split(',');
    const nuevaDir = actual === campo && dir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }
}