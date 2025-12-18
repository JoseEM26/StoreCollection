// src/app/pages/administrativo/products/product-form/product-form.component.ts

import { Component, EventEmitter, Input, Output, signal, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminService } from '../../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../../auth/auth.service';
import { CategoriaAdminService } from '../../../../service/service-admin/categoria-admin.service';
import {
  ProductoResponse,
  ProductoRequest,
  VarianteRequest,
  AtributoValorRequest
} from '../../../../model/admin/producto-admin.model';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit {
  @Input() isEdit = false;
  @Input() producto?: ProductoResponse;

  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  // Datos principales
  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);

  // Dropdowns
  tiendas = signal<DropTownStandar[]>([]);
  categorias = signal<DropTownStandar[]>([]);

  // Variantes
  variantes = signal<VarianteRequest[]>([]);

  loading = signal(false);
  errorMessage = signal<string>('');

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService,
    private categoriaService: CategoriaAdminService
  ) {}

  ngOnInit(): void {
    // Cargar tiendas solo si es ADMIN
    if (this.auth.isAdmin()) {
      this.dropTownService.getTiendas().subscribe({
        next: (data) => this.tiendas.set(data),
        error: () => this.errorMessage.set('Error al cargar tiendas')
      });
    }

    // Cargar categorías (filtradas por tienda si es necesario)
    this.loadCategorias();
  }

  ngOnChanges(): void {
    if (this.isEdit && this.producto) {
      this.nombre.set(this.producto.nombre);
      this.slug.set(this.producto.slug);
      this.categoriaId.set(this.producto.categoriaId);

      if (this.auth.isAdmin()) {
        this.tiendaId.set(this.producto.tiendaId);
      }

      // Cargar variantes existentes
      this.variantes.set(
        this.producto.variantes.map(v => ({
          id: v.id,
          sku: v.sku,
          precio: v.precio,
          stock: v.stock,
          imagenUrl: v.imagenUrl || '',
          atributos: v.atributos.map(a => ({
            atributoNombre: a.atributoNombre,
            valor: a.valor
          }))
        }))
      );
    } else {
      this.resetForm();
    }
    this.errorMessage.set('');
  }

  // Cuando ADMIN cambia de tienda, recargar categorías de esa tienda
  onTiendaChange(): void {
    this.categoriaId.set(null);
    this.loadCategorias();
  }

  private loadCategorias(): void {
    // Si tienes un endpoint que filtre categorías por tienda, úsalo
    // Por ahora asumimos que carga todas o las del usuario
    // Si quieres filtrar por tiendaId cuando es ADMIN, crea un método en CategoriaAdminService
    this.categoriaService.listarCategorias(0, 1000, 'nombre,asc').subscribe({
      next: (page) => {
        const cats = page.content.map(c => ({
          id: c.id,
          descripcion: c.nombre
        }));
        this.categorias.set(cats);
      }
    });
  }

  // === Variantes ===
  agregarVariante(): void {
    this.variantes.update(vs => [...vs, {
      sku: '',
      precio: 0,
      stock: 0,
      imagenUrl: '',
      atributos: []
    }]);
  }

  eliminarVariante(index: number): void {
    this.variantes.update(vs => vs.filter((_, i) => i !== index));
  }

  agregarAtributo(varianteIndex: number): void {
    this.variantes.update(vs => {
      const newVs = [...vs];
      newVs[varianteIndex].atributos = newVs[varianteIndex].atributos || [];
      newVs[varianteIndex].atributos!.push({ atributoNombre: '', valor: '' });
      return newVs;
    });
  }

  eliminarAtributo(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(vs => {
      const newVs = [...vs];
      newVs[varianteIndex].atributos!.splice(attrIndex, 1);
      return newVs;
    });
  }

  // Generar slug automático desde nombre
  generarSlugDesdeNombre(): void {
    const slug = this.nombre()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    this.slug.set(slug);
  }

  save(): void {
    const nombre = this.nombre().trim();
    const slug = this.slug().trim();

    if (!nombre || !slug || !this.categoriaId()) {
      this.errorMessage.set('Nombre, slug y categoría son obligatorios');
      return;
    }

    if (this.auth.isAdmin() && !this.tiendaId()) {
      this.errorMessage.set('Debes seleccionar una tienda');
      return;
    }

    if (this.variantes().length === 0) {
      this.errorMessage.set('Debe agregar al menos una variante');
      return;
    }

    // Validar variantes básicas
    for (const v of this.variantes()) {
      if (!v.sku.trim() || v.precio <= 0 || v.stock < 0) {
        this.errorMessage.set('Todas las variantes deben tener SKU válido, precio > 0 y stock ≥ 0');
        return;
      }
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const request: ProductoRequest = {
      nombre,
      slug,
      categoriaId: this.categoriaId()!,
      variantes: this.variantes()
    };

    if (this.auth.isAdmin()) {
      request.tiendaId = this.tiendaId()!;
    }

    const obs = this.isEdit && this.producto
      ? this.productoService.actualizarProducto(this.producto.id, request)
      : this.productoService.crearProducto(request);

    obs.subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Error al guardar el producto');
      }
    });
  }

  private resetForm(): void {
    this.nombre.set('');
    this.slug.set('');
    this.categoriaId.set(null);
    this.tiendaId.set(null);
    this.variantes.set([{
      sku: '',
      precio: 0,
      stock: 0,
      imagenUrl: '',
      atributos: []
    }]);
  }

  cancel(): void {
    this.closed.emit();
  }
}