// src/app/pages/administrativo/products/product-form/product-form.component.ts

import { Component, EventEmitter, Input, Output, signal, computed, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminService } from '../../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../../auth/auth.service';
import { CategoriaAdminService } from '../../../../service/service-admin/categoria-admin.service';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';
import {
  ProductoResponse,
  ProductoRequest,
  VarianteRequest,
  AtributoValorRequest
} from '../../../../model/admin/producto-admin.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit, OnChanges {
  @Input() isEdit = false;
  @Input() producto?: ProductoResponse;

  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  // Datos principales
  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);
  activo = signal<boolean>(true);  // Nuevo: para producto activo/inactivo

  // Dropdowns
  tiendas = signal<DropTownStandar[]>([]);
  categorias = signal<DropTownStandar[]>([]);

  // Variantes con atributos como texto
  variantes = signal<VarianteRequest[]>([]);

  // Variantes ordenadas: por Talla (numérica), luego por Color
  variantesOrdenadas = computed(() => {
    return [...this.variantes()].sort((a, b) => {
      const tallaA = this.getValorAtributo(a, 'Talla');
      const tallaB = this.getValorAtributo(b, 'Talla');
      const numA = parseInt(tallaA || '0');
      const numB = parseInt(tallaB || '0');
      if (!isNaN(numA) && !isNaN(numB)) {
        if (numA !== numB) return numA - numB;
      } else if (!isNaN(numA)) return -1;
      else if (!isNaN(numB)) return 1;

      const colorA = this.getValorAtributo(a, 'Color') || '';
      const colorB = this.getValorAtributo(b, 'Color') || '';
      return colorA.localeCompare(colorB);
    });
  });

  loading = signal(false);
  errorMessage = signal<string>('');

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService,
    private categoriaService: CategoriaAdminService
  ) {}

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.dropTownService.getTiendas().subscribe({
        next: (data: DropTownStandar[]) => this.tiendas.set(data),
        error: () => this.errorMessage.set('Error al cargar tiendas')
      });
    }
    this.loadCategorias();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.errorMessage.set('');

    if (this.isEdit && this.producto) {
      this.nombre.set(this.producto.nombre || '');
      this.slug.set(this.producto.slug || '');
      this.categoriaId.set(this.producto.categoriaId || null);
      this.activo.set(this.producto.activo);  // Cargar activo del producto

      if (this.auth.isAdmin()) {
        this.tiendaId.set(this.producto.tiendaId || null);
      }

      // Mapear variantes completas con fallback a array vacío
      const variantesDelBackend = this.producto.variantes ?? [];

      this.variantes.set(
        variantesDelBackend.map(v => ({
          id: v.id,
          sku: v.sku || '',
          precio: v.precio || 0,
          stock: v.stock || 0,
          imagenUrl: v.imagenUrl || '',
          activo: v.activo,  // Cargar activo de cada variante
          atributos: (v.atributos ?? []).map(a => ({
            atributoNombre: a.atributoNombre || '',
            valor: a.valor || ''
          }))
        }))
      );

      // Si admin, recargar categorías basadas en tienda
      if (this.auth.isAdmin() && this.tiendaId()) {
        this.onTiendaChange();
      }
    } else {
      this.resetForm();
    }
  }

  onTiendaChange(): void {
    this.categoriaId.set(null);
    this.loadCategorias();
  }

  private loadCategorias(): void {
    this.categoriaService.listarCategorias(0, 1000, 'nombre,asc').subscribe({
      next: (page: any) => {
        const cats = page.content.map((c: any) => ({
          id: c.id,
          descripcion: c.nombre
        }));
        this.categorias.set(cats);
      }
    });
  }

  // === VARIANTE HELPERS ===
  agregarVariante(): void {
    this.variantes.update(vs => [...vs, {
      sku: '',
      precio: 0,
      stock: 0,
      imagenUrl: '',
      activo: true,
      atributos: [
        { atributoNombre: 'Talla', valor: '' },
        { atributoNombre: 'Color', valor: '' }
      ]
    }]);
  }

  eliminarVariante(index: number): void {
    this.variantes.update(vs => vs.filter((_, i) => i !== index));
  }

  agregarAtributo(varianteIndex: number): void {
    this.variantes.update(vs => {
      const newVs = [...vs];
      newVs[varianteIndex].atributos.push({ atributoNombre: '', valor: '' });
      return newVs;
    });
  }

  eliminarAtributo(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(vs => {
      const newVs = [...vs];
      newVs[varianteIndex].atributos.splice(attrIndex, 1);
      return newVs;
    });
  }

  getValorAtributo(variante: VarianteRequest, nombreAttr: string): string {
    const attr = variante.atributos.find(a => 
      a.atributoNombre.toLowerCase() === nombreAttr.toLowerCase()
    );
    return attr?.valor || '';
  }

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
    if (!this.nombre().trim() || !this.slug().trim() || !this.categoriaId()) {
      this.errorMessage.set('Nombre, slug y categoría son obligatorios');
      return;
    }
    if (this.auth.isAdmin() && !this.tiendaId()) {
      this.errorMessage.set('Selecciona una tienda');
      return;
    }
    if (this.variantes().length === 0) {
      this.errorMessage.set('Agrega al menos una variante');
      return;
    }

    const invalid = this.variantes().some(v =>
      !v.sku.trim() || v.precio <= 0 || v.stock < 0 ||
      v.atributos.some(a => !a.atributoNombre.trim() || !a.valor.trim())
    );
    if (invalid) {
      this.errorMessage.set('Completa todos los campos de variantes y atributos');
      return;
    }

    this.loading.set(true);

    const request: ProductoRequest = {
      nombre: this.nombre().trim(),
      slug: this.slug().trim(),
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
        this.errorMessage.set(err.error?.message || 'Error al guardar');
      }
    });
  }

  private resetForm(): void {
    this.nombre.set('');
    this.slug.set('');
    this.categoriaId.set(null);
    this.tiendaId.set(null);
    this.activo.set(true);
    this.variantes.set([{
      sku: '',
      precio: 0,
      stock: 0,
      imagenUrl: '',
      activo: true,
      atributos: [
        { atributoNombre: 'Talla', valor: '' },
        { atributoNombre: 'Color', valor: '' }
      ]
    }]);
  }

  cancel(): void {
    this.closed.emit();
  }
}