// src/app/pages/administrativo/products/products.component.ts

import { Component, OnInit, signal, effect, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminListItem, ProductoAdminListPage, ProductoPage, ProductoResponse } from '../../../model/admin/producto-admin.model';
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
pageData = signal<ProductoAdminListPage | null>(null);
  productos = signal<ProductoAdminListItem[]>([]);  // ← Tipo correcto ahora
  loading = signal(true);
  // Modal
  showModal = signal(false);
  isEditMode = signal(false);
  selectedProducto = signal<ProductoResponse | undefined>(undefined);
  loadingEdicion = signal(false);
// === AÑADE ESTOS NUEVOS SIGNALS cerca de los otros modales ===
showDetailModal = signal(false);              // Nuevo: controla el modal de detalle
selectedProductoDetail = signal<ProductoResponse | undefined>(undefined); // Producto en modo detalle
loadingDetail = signal(false);                 // Loading mientras carga el detalle
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
openDetailModal(id: number): void {
  this.loadingDetail.set(true);
  this.showDetailModal.set(true);

  this.productoService.obtenerParaEdicion(id).subscribe({
    next: (productoCompleto) => {
      this.selectedProductoDetail.set(productoCompleto);
      this.loadingDetail.set(false);
    },
    error: (err) => {
      this.loadingDetail.set(false);
      this.showDetailModal.set(false);
      this.mostrarError(err, 'No se pudo cargar el detalle del producto');
    }
  });
}
handleImageError(event: Event): void {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = 'https://via.placeholder.com/80?text=Sin+imagen';
  }
}
// === MÉTODO PARA CERRAR EL MODAL DE DETALLE ===
closeDetailModal(): void {
  this.showDetailModal.set(false);
  this.loadingDetail.set(false);
  this.selectedProductoDetail.set(undefined);
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
formatPrecio(min: number, max: number): string {
    const fmt = (n: number) => n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (min === max) {
      return `S/ ${fmt(min)}`;
    }
    return `S/ ${fmt(min)} - S/ ${fmt(max)}`;
  }

  // Badge de stock
  getStockBadgeClass(stock: number): string {
    if (stock === 0) return 'bg-danger text-white';
    if (stock <= 10) return 'bg-warning text-dark';
    return 'bg-success text-white';
  }

  getStockText(stock: number): string {
    if (stock === 0) return 'Sin stock';
    if (stock <= 10) return `${stock} en stock`;
    return `${stock} disponibles`;
  }
toggleActivo(producto: ProductoAdminListItem): void {
  const nuevoEstado = !producto.activo;

  this.loadingToggleId.set(producto.id);

  // Optimistic update
  this.productos.update(list =>
    list.map(p => p.id === producto.id ? { ...p, activo: nuevoEstado } : p)
  );

  this.productoService.toggleActivo(producto.id).subscribe({
    next: (updatedResponse: ProductoResponse) => {
      // Buscamos el producto actual en la lista para conservar las propiedades extras
      this.productos.update(list =>
        list.map(p =>
          p.id === updatedResponse.id
            ? { ...p, ...updatedResponse }  // merge: mantiene las props extras y actualiza las del response
            : p
        )
      );

      this.loadingToggleId.set(null);

      Swal.fire({
        icon: 'success',
        title: nuevoEstado ? 'Activado' : 'Desactivado',
        text: `El producto "${updatedResponse.nombre}" ahora está ${nuevoEstado ? 'activo' : 'inactivo'}.`,
        timer: 1500,
        showConfirmButton: false
      });
    },
    error: (err) => {
      this.productos.update(list =>
        list.map(p => p.id === producto.id ? { ...p, activo: !nuevoEstado } : p)
      );
      this.loadingToggleId.set(null);
      this.mostrarError(err, 'Error al cambiar el estado del producto');
    }
  });
}
  // ======================== UTILIDADES ========================

 trackByProductoId(index: number, producto: ProductoAdminListItem): unknown {
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