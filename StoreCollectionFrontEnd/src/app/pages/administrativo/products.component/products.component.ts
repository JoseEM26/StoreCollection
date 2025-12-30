// src/app/pages/administrativo/products/products.component.ts

import { Component, OnInit, signal, effect, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPage, ProductoResponse } from '../../../model/admin/producto-admin.model';
import { ProductoAdminService } from '../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { ProductFormComponent } from './product-form/product-form.component';
import Swal from 'sweetalert2';

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

  // Loading para toggle (evita clicks múltiples)
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
    // Recarga cuando cambian filtros o página
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
    return producto.variantes.length === 1 ? '1 variante' : `${producto.variantes.length} variantes`;
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
      error: (err) => {
        this.loading.set(false);
        this.mostrarError(err, 'Error al cargar los productos');
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
        this.loadingEdicion.set(false);
        this.showModal.set(false);
        this.mostrarError(err, 'No se pudo cargar el producto para editar');
      }
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.loadingEdicion.set(false);
  }

  onProductSaved(): void {
    Swal.fire({
      icon: 'success',
      title: '¡Guardado!',
      text: 'El producto se ha guardado correctamente.',
      timer: 2000,
      showConfirmButton: false
    });
    this.closeModal();
    this.currentPage.set(0); // Volver a página 1 tras crear/editar
    this.loadProductos();
  }

  toggleActivo(producto: ProductoResponse): void {
    const nuevoEstado = !producto.activo;

    // Loading en el botón
    this.loadingToggleId.set(producto.id);

    // Optimistic UI update
    this.productos.update(list =>
      list.map(p => p.id === producto.id ? { ...p, activo: nuevoEstado } : p)
    );

    this.productoService.toggleActivo(producto.id).subscribe({
      next: (updated) => {
        this.productos.update(list =>
          list.map(p => p.id === updated.id ? updated : p)
        );
        this.loadingToggleId.set(null);

        Swal.fire({
          icon: 'success',
          title: nuevoEstado ? 'Activado' : 'Desactivado',
          text: `El producto "${updated.nombre}" ahora está ${nuevoEstado ? 'activo' : 'inactivo'}.`,
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        // Revertir cambio optimista
        this.productos.update(list =>
          list.map(p => p.id === producto.id ? { ...p, activo: !nuevoEstado } : p)
        );
        this.loadingToggleId.set(null);
        this.mostrarError(err, 'Error al cambiar el estado del producto');
      }
    });
  }

  // ======================== UTILIDADES ========================

  trackByProductoId(index: number, producto: ProductoResponse): number {
    return producto.id;
  }

  onSearch(): void {
    this.currentPage.set(0);
  }

  setSort(campo: string): void {
    const [actualCampo, actualDir] = this.sort().split(',');
    const nuevaDir = actualCampo === campo && actualDir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }

  goToPage(page: number): void {
    const totalPages = this.pageData()?.totalPages || 0;
    if (page >= 0 && page < totalPages) {
      this.currentPage.set(page);
    }
  }

  getPageNumbers(): number[] {
    const total = this.pageData()?.totalPages || 1;
    const current = this.currentPage();
    const delta = 2;
    const left = Math.max(0, current - delta);
    const right = Math.min(total - 1, current + delta);
    const range = [];

    if (left > 0) range.push(0); // Siempre mostrar primera página
    if (left > 1) range.push(-1); // ... para indicar salto

    for (let i = left; i <= right; i++) {
      range.push(i);
    }

    if (right < total - 2) range.push(-1); // ...
    if (right < total - 1) range.push(total - 1); // Última página

    return range;
  }

  // ======================== MANEJO DE ERRORES CON SWEETALERT2 ========================

  private mostrarError(err: any, tituloDefault: string = 'Error') {
    const mensaje = err.error?.message || err.message || 'Error desconocido';
    const titulo = err.error?.error || tituloDefault;

    Swal.fire({
      icon: 'error',
      title: titulo,
      text: mensaje,
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#d33'
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showModal()) {
      this.closeModal();
    }
  }
}