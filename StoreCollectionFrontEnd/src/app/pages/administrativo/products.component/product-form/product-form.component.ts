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
  activo = signal<boolean>(true);

  // Dropdowns
  tiendas = signal<DropTownStandar[]>([]);
  categorias = signal<DropTownStandar[]>([]);

  // Variantes
  variantes = signal<VarianteRequest[]>([]);

  // Control de colapso de cada variante (true = cerrada, false = abierta)
  collapsed = signal<boolean[]>([]);

  // Variantes ordenadas por Talla (numérica) y luego por Color
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

    if (changes['producto'] || changes['isEdit']) {
      if (this.isEdit && this.producto) {
        this.nombre.set(this.producto.nombre || '');
        this.slug.set(this.producto.slug || '');
        this.categoriaId.set(this.producto.categoriaId || null);
        this.activo.set(this.producto.activo ?? true);

        if (this.auth.isAdmin()) {
          this.tiendaId.set(this.producto.tiendaId || null);
        }

        // Mapear variantes
        const variantesDelBackend = this.producto.variantes ?? [];
        this.variantes.set(
          variantesDelBackend.map(v => ({
            id: v.id,
            sku: v.sku || '',
            precio: v.precio || 0,
            stock: v.stock || 0,
            imagenUrl: v.imagenUrl || '',
            activo: v.activo ?? true,
            atributos: (v.atributos ?? []).map(a => ({
              atributoNombre: a.atributoNombre || '',
              valor: a.valor || ''
            }))
          }))
        );

        // Reiniciar estado de colapso
        this.initCollapsed();

        if (this.categorias().length === 0) {
          this.loadCategorias();
        }

      } else {
        this.resetForm();
      }
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

  // === GESTIÓN DE COLAPSO ===
  toggleCollapse(index: number): void {
    this.collapsed.update(arr => {
      const newArr = [...arr];
      newArr[index] = !newArr[index];
      return newArr;
    });
  }

  private initCollapsed(): void {
    const length = this.variantes().length;
    if (length === 0) {
      this.collapsed.set([]);
    } else {
      // Primera variante abierta (false), las demás cerradas (true)
      this.collapsed.set(Array(length).fill(true).map((_, i) => i !== 0));
    }
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

    // Nueva variante agregada cerrada
    this.collapsed.update(arr => [...arr, true]);
  }

  eliminarVariante(index: number): void {
    this.variantes.update(vs => vs.filter((_, i) => i !== index));
    this.collapsed.update(arr => arr.filter((_, i) => i !== index));
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
      variantes: this.variantes(),
      activo: this.activo()  // Si tu backend lo acepta en el request
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
    this.collapsed.set([false]); // Primera variante abierta
  }

  cancel(): void {
    this.closed.emit();
  }
}