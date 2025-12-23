// src/app/pages/administrativo/products/products.component.ts

import { Component, OnInit, signal, effect, HostListener } from '@angular/core';
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
  loadingEdicion = signal(false);

  // Loading para el botón de toggle (evita clicks múltiples)
  loadingToggleId = signal<number | null>(null);

  // Filtros y paginación
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = signal<string>('');

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    // Recarga automática cuando cambian filtros relevantes
    effect(() => {
      this.loadProductos();
    });
  }

  ngOnInit(): void {
    this.loadProductos();
  }

  getVarianteText(producto: ProductoResponse): string {
    if (!producto.variantes || producto.variantes.length === 0) {
      return 'Sin variantes';
    }
    if (producto.variantes.length === 1) {
      return '1 variante';
    }
    return `${producto.variantes.length} variantes`;
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

  openCreateModal(): void {
    this.isEditMode.set(false);
    this.selectedProducto.set(undefined);
    this.showModal.set(true);
  }

  openEditModal(id: number): void {
    this.loadingEdicion.set(true);
    this.isEditMode.set(true);
    this.showModal.set(true);

    this.productoService.obtenerParaEdicion(id).subscribe({
      next: (productoCompleto) => {
        this.selectedProducto.set(productoCompleto);
        this.loadingEdicion.set(false);
      },
      error: (err) => {
        console.error('Error al cargar producto para edición', err);
        this.loadingEdicion.set(false);
        this.showModal.set(false);
        alert('No se pudo cargar el producto.');
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

  toggleActivo(producto: ProductoResponse): void {
    const nuevoEstado = !producto.activo;

    // Loading en el botón específico
    this.loadingToggleId.set(producto.id);

    // Optimistic update: cambia inmediatamente en la UI
    this.productos.update(list =>
      list.map(p =>
        p.id === producto.id ? { ...p, activo: nuevoEstado } : p
      )
    );

    // Llamada al backend
    this.productoService.toggleActivo(producto.id).subscribe({
      next: (updatedProducto: ProductoResponse) => {
        // Actualiza con el objeto real devuelto por el backend (fuente de verdad)
        this.productos.update(list =>
          list.map(p =>
            p.id === updatedProducto.id ? updatedProducto : p
          )
        );
        this.loadingToggleId.set(null);
      },
      error: (err) => {
        console.error('Error al cambiar estado', err);
        alert('Error al cambiar el estado del producto');

        // Revierte el cambio optimista si falla
        this.productos.update(list =>
          list.map(p =>
            p.id === producto.id ? { ...p, activo: !nuevoEstado } : p
          )
        );
        this.loadingToggleId.set(null);
      }
    });
  }

  trackByProductoId(index: number, producto: ProductoResponse): number {
    return producto.id;
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

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showModal()) {
      this.closeModal();
    }
  }
}