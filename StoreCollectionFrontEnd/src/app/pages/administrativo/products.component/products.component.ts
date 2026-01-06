// src/app/pages/administrativo/products/products.component.ts
import { Component, OnInit, signal, effect, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminListItem, ProductoAdminListPage, ProductoResponse } from '../../../model/admin/producto-admin.model';
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
  // Signals principales
  pageData = signal<ProductoAdminListPage | null>(null);
  productos = signal<ProductoAdminListItem[]>([]);
  loading = signal(true);

  // Modales
  showModal = signal(false);
  isEditMode = signal(false);
  selectedProducto = signal<ProductoResponse | undefined>(undefined);
  loadingEdicion = signal(false);

  showDetailModal = signal(false);
  selectedProductoDetail = signal<ProductoResponse | undefined>(undefined);
  loadingDetail = signal(false);

  // Loading para toggle (evita múltiples clics)
  loadingToggleId = signal<number | null>(null);

  // Filtros y paginación
  currentPage = signal(0);
  pageSize = 20;
  sort = signal<string>('nombre,asc');
  searchTerm = signal<string>('');

  // Flag para evitar bucle infinito en imágenes fallidas
  private failedImages = new Set<string>(); // Usamos src como key para evitar repeticiones

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    // Efecto reactivo para recarga automática
    effect(() => {
      this.loadProductos();
    });
  }

  ngOnInit(): void {
    this.loadProductos();
  }

  // ===============================================
  // Carga de productos
  // ===============================================
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

  // ===============================================
  // Manejo de errores de imagen (SOLUCIÓN AL BUCLE INFINITO)
  // ===============================================
  handleImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    const currentSrc = img.src;

    // Evitar bucle infinito: si ya es la imagen por defecto, no hacer nada
    if (this.failedImages.has(currentSrc)) {
      return;
    }

    this.failedImages.add(currentSrc);

    // Usa una imagen local (recomendado) o base64 ligero
    img.src = 'https://res.cloudinary.com/dqznlmig0/image/upload/v1767658215/imagen_2026-01-05_191004692_bepdxz.png'; // ← Crea esta imagen en assets (80x80 px)

    // Alternativa base64 muy ligera (gris con texto) - sin llamadas externas
    // img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0iI2ZmZmZmZiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZTVlN2ViIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMTQiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGFsaWdubWVudC1iYXNlbGluZT0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5TaW4gaW1hZ2VuPC90ZXh0Pjwvc3ZnPg==';

    img.alt = 'Sin imagen disponible';
  }

  // ===============================================
  // Utilidades visuales
  // ===============================================
  getVarianteText(producto: ProductoResponse): string {
    if (!producto.variantes || producto.variantes.length === 0) return 'Sin variantes';
    return producto.variantes.length === 1 ? '1 variante' : `${producto.variantes.length} variantes`;
  }

  formatPrecio(min: number, max: number): string {
    const fmt = (n: number) => n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return min === max ? `S/ ${fmt(min)}` : `S/ ${fmt(min)} - S/ ${fmt(max)}`;
  }

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

  // ===============================================
  // Acciones principales
  // ===============================================
  openDetailModal(id: number): void {
    this.loadingDetail.set(true);
    this.showDetailModal.set(true);

    this.productoService.obtenerParaEdicion(id).subscribe({
      next: (producto) => {
        this.selectedProductoDetail.set(producto);
        this.loadingDetail.set(false);
      },
      error: (err) => {
        this.loadingDetail.set(false);
        this.showDetailModal.set(false);
        this.mostrarError(err, 'No se pudo cargar el detalle del producto');
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
      next: (producto) => {
        this.selectedProducto.set(producto);
        this.loadingEdicion.set(false);
      },
      error: (err) => {
        this.loadingEdicion.set(false);
        this.showModal.set(false);
        this.mostrarError(err, 'No se pudo cargar el producto para editar');
      }
    });
  }

  toggleActivo(producto: ProductoAdminListItem): void {
    if (this.loadingToggleId() === producto.id) return; // Evita doble clic

    const nuevoEstado = !producto.activo;
    this.loadingToggleId.set(producto.id);

    // Optimistic update
    this.productos.update(list =>
      list.map(p => p.id === producto.id ? { ...p, activo: nuevoEstado } : p)
    );

    this.productoService.toggleActivo(producto.id).subscribe({
      next: (updated) => {
        this.productos.update(list =>
          list.map(p => p.id === updated.id ? { ...p, ...updated } : p)
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
        // Rollback optimistic update
        this.productos.update(list =>
          list.map(p => p.id === producto.id ? { ...p, activo: !nuevoEstado } : p)
        );
        this.loadingToggleId.set(null);
        this.mostrarError(err, 'Error al cambiar estado');
      }
    });
  }

  // ===============================================
  // Modal handlers
  // ===============================================
  closeModal(): void {
    this.showModal.set(false);
    this.loadingEdicion.set(false);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.loadingDetail.set(false);
    this.selectedProductoDetail.set(undefined);
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
    this.currentPage.set(0);
    this.loadProductos();
  }

  // ===============================================
  // Utilidades generales
  // ===============================================
  trackByProductoId(index: number, producto: ProductoAdminListItem): number {
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
    const range: number[] = [];

    if (left > 0) range.push(0);
    if (left > 1) range.push(-1); // ...

    for (let i = left; i <= right; i++) {
      range.push(i);
    }

    if (right < total - 2) range.push(-1);
    if (right < total - 1) range.push(total - 1);

    return range;
  }

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
  onEscape(): void {
    if (this.showModal()) this.closeModal();
    if (this.showDetailModal()) this.closeDetailModal();
  }
}