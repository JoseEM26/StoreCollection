// src/app/pages/administrativo/products/products.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPage, ProductoResponse } from '../../../model/admin/producto-admin.model';
import { ProductoAdminService } from '../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent implements OnInit {
  // Datos del backend
  pageData = signal<ProductoPage | null>(null);
  productos = signal<ProductoResponse[]>([]);
  loading = signal(true);

  // Filtros
  currentPage = signal(0);
  pageSize = 20;
  sort = signal('nombre,asc');
  searchTerm = '';

  // Modal
  showModal = false;
  editingProduct: Partial<ProductoResponse> = {};

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService
  ) {
    effect(() => this.loadProductos());
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
        alert('Error al cargar productos');
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
  setSort(campo: string): void {
    const [actual, dir] = this.sort().split(',');
    const nuevaDir = actual === campo && dir === 'asc' ? 'desc' : 'asc';
    this.sort.set(`${campo},${nuevaDir}`);
    this.currentPage.set(0);
  }

  // Modal
  openModal(product?: ProductoResponse): void {
    this.editingProduct = product ? { ...product } : {
      nombre: '',
      slug: '',
      categoriaId: 0,
      categoriaNombre: '',
      tiendaId: 0
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveProduct(): void {
    if (!this.editingProduct.nombre?.trim()) {
      alert('El nombre es obligatorio');
      return;
    }
    alert(`Producto "${this.editingProduct.nombre}" guardado (simulado)`);
    this.closeModal();
    this.loadProductos();
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