// src/app/pages/administrativo/products/product-form/product-form.component.ts
import { Component, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoAdminService } from '../../../../service/service-admin/producto-admin.service';
import { CategoriaDropdown } from '../../../../model/admin/categoria-admin.model';
import { AtributoConValores } from '../../../../model/admin/atributo-dropdown.model';
import { ProductoResponse, VarianteResponse } from '../../../../model/admin/producto-admin.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent {
  productId = input<number | null>(null);
  isEdit = input<boolean>(false);

  close = output<void>();
  saved = output<void>();

  // Formulario
  nombre = signal('');
  slug = signal('');
  categoriaId = signal<number | null>(null);

  categorias = signal<CategoriaDropdown[]>([]);
  atributos = signal<AtributoConValores[]>([]);

  variantes = signal<VarianteForm[]>([this.nuevaVarianteVacia()]);

  loading = signal(false);
  saving = signal(false);

  constructor(private productoService: ProductoAdminService) {
    // Cargar categorías y atributos una sola vez
    this.productoService.obtenerCategorias().subscribe(c => this.categorias.set(c));
    this.productoService.obtenerAtributos().subscribe(a => this.atributos.set(a));

    // Cargar producto si es edición
    effect(() => {
      const id = this.productId();
      if (id && this.isEdit()) {
        this.cargarProducto(id);
      }
    });
  }

  cargarProducto(id: number) {
    this.loading.set(true);
    this.productoService.obtenerProducto(id).subscribe({
      next: (prod) => {
        this.nombre.set(prod.nombre);
        this.slug.set(prod.slug);
        this.categoriaId.set(prod.categoriaId);

        const vars = prod.variantes.map(v => this.mapVarianteResponseToForm(v));
        this.variantes.set(vars.length > 0 ? vars : [this.nuevaVarianteVacia()]);
        this.loading.set(false);
      },
      error: () => {
        alert('Error al cargar el producto');
        this.loading.set(false);
      }
    });
  }

  private mapVarianteResponseToForm(v: VarianteResponse): VarianteForm {
    const attrs: Record<string, string> = {};
    v.atributos.forEach(a => {
      attrs[a.nombreAtributo] = a.valor;
    });

    return {
      id: v.id,
      sku: v.sku,
      precio: v.precio,
      stock: v.stock,
      imagenUrl: v.imagenUrl || '',
      activo: v.activo,
      atributosSeleccionados: attrs
    };
  }

  generateSlug() {
    if (!this.slug().trim() && this.nombre().trim()) {
      this.slug.set(this.toSlug(this.nombre()));
    }
  }

  private toSlug(text: string): string {
    return text.toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-');
  }

  nuevaVarianteVacia(): VarianteForm {
    return {
      id: undefined,
      sku: '',
      precio: 0,
      stock: 0,
      imagenUrl: '',
      activo: true,
      atributosSeleccionados: {}
    };
  }

  agregarVariante() {
    this.variantes.update(v => [...v, this.nuevaVarianteVacia()]);
  }

  eliminarVariante(index: number) {
    if (this.variantes().length > 1) {
      this.variantes.update(v => v.filter((_, i) => i !== index));
    }
  }

  guardar() {
    if (!this.nombre().trim()) {
      alert('El nombre del producto es obligatorio');
      return;
    }
    if (!this.categoriaId()) {
      alert('Debes seleccionar una categoría');
      return;
    }

    const request = {
      id: this.productId() || undefined,
      nombre: this.nombre().trim(),
      slug: (this.slug().trim() || this.toSlug(this.nombre())).trim(),
      categoriaId: this.categoriaId()!,
      variantes: this.variantes().map(v => ({
        id: v.id,
        sku: v.sku.trim() || this.generarSkuAutomatico(),
        precio: Number(v.precio),
        stock: Number(v.stock) || 0,
        imagenUrl: v.imagenUrl.trim() || undefined,
        activo: v.activo,
        atributoValorIds: this.obtenerIdsAtributos(v.atributosSeleccionados)
      })).filter(v => v.sku && v.precio > 0)
    };

    if (request.variantes.length === 0) {
      alert('Debes tener al menos una variante válida');
      return;
    }

    this.saving.set(true);
    const obs = this.isEdit()
      ? this.productoService.actualizarProducto(this.productId()!, request)
      : this.productoService.crearProducto(request);

    obs.subscribe({
      next: () => {
        alert(`Producto ${this.isEdit() ? 'actualizado' : 'creado'} correctamente`);
        this.saved.emit();
        this.close.emit();
      },
      error: (err) => {
        console.error(err);
        alert(err.error?.message || 'Error al guardar el producto');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private generarSkuAutomatico(): string {
    const base = this.slug().trim() || 'prod';
    const num = this.variantes().length + 1;
    return `${base.toUpperCase()}-${String(num).padStart(3, '0')}`;
  }

  private obtenerIdsAtributos(seleccionados: Record<string, string>): number[] {
    const ids: number[] = [];
    for (const [nombreAttr, valor] of Object.entries(seleccionados)) {
      if (!valor) continue;
      const attr = this.atributos().find(a => a.nombre === nombreAttr);
      const val = attr?.valores.find(v => v.valor === valor);
      if (val) ids.push(val.id);
    }
    return ids;
  }

  getValoresAtributo(nombre: string) {
    return this.atributos().find(a => a.nombre === nombre)?.valores || [];
  }
}

interface VarianteForm {
  id?: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl: string;
  activo: boolean;
  atributosSeleccionados: Record<string, string>;
}