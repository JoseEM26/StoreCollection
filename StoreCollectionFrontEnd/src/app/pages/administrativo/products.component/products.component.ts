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
  pageData = signal<ProductoPage | null>(null);
  productos = signal<ProductoResponse[]>([]);
  loading = signal(true);

  // Modal
  showModal = signal(false);
  isEditMode = signal(false);
  selectedProducto = signal<ProductoResponse | undefined>(undefined);

  // Loading específico para edición
  loadingEdicion = signal(false);
getVarianteText(producto: ProductoResponse): string {
  if (!producto.variantes || producto.variantes.length === 0) {
    return 'Sin variantes';
  }
  if (producto.variantes.length === 1) {
    return '1 variante';
  }
  return `${producto.variantes.length} variantes`;
}
  // Filtros
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = signal<string>('');

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    effect(() => {
      this.loadProductos();
    });
  }

  ngOnInit(): void {
    this.loadProductos();
  }
onImageError(event: any) {
  event.target.src = 'https://via.placeholder.com/80?text=📦';
}
  loadProductos(): void {
    this.loading.set(true);

    this.productoService.listarProductos(
      this.currentPage(),
      this.pageSize,
      this.sort(),
      this.searchTerm().trim() || undefined
    ).subscribe({
      next: (data) => {
        this.pageData.set(data);
        this.productos.set(data.content || []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        alert('Error al cargar productos');
      }
    });
  }

  // === CREAR ===
  openCreateModal(): void {
    this.isEditMode.set(false);
    this.selectedProducto.set(undefined);
    this.showModal.set(true);
  }

  // === EDITAR: LLAMADA FRESCA AL ENDPOINT DE EDICIÓN ===
  openEditModal(id: number): void {
    this.loadingEdicion.set(true);
    this.isEditMode.set(true);
    this.showModal.set(true);

    this.productoService.obtenerParaEdicion(id).subscribe({
      next: (productoCompleto) => {
        console.log('Producto cargado para edición:', productoCompleto); // ← Verás todo el JSON con variantes
        this.selectedProducto.set(productoCompleto);
        this.loadingEdicion.set(false);
      },
      error: (err) => {
        console.error('Error al cargar producto para edición', err);
        this.loadingEdicion.set(false);
        this.showModal.set(false);
        alert('No se pudo cargar el producto. Verifica permisos o conexión.');
      }
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.loadingEdicion.set(false);
  }

  onProductSaved(): void {
    this.closeModal();
    this.currentPage.set(0);
    this.loadProductos();
  }

  // === TOGGLE ACTIVO ===
  toggleActivo(producto: ProductoResponse): void {
    if (!this.auth.isAdmin()) return;

    this.productoService.toggleActivo(producto.id).subscribe({
      next: (updated) => {
        this.productos.update(list => list.map(p => p.id === updated.id ? updated : p));
      }
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
  }

  setSort(campo: string): void {
    const [actual, dir] = this.sort().split(',');
    const nuevaDir = actual === campo && dir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
  }

  goToPage(page: number): void {
    const total = this.pageData()?.totalPages || 0;
    if (page >= 0 && page < total) {
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
}