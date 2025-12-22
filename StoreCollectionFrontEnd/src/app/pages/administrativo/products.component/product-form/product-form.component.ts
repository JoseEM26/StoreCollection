import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AtributoConValores,
  ProductoResponse,
  VarianteRequest,
  AtributoValorRequest
} from '../../../../model/admin/producto-admin.model';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';
import { ProductoAdminService } from '../../../../service/service-admin/producto-admin.service';
import { AuthService } from '../../../../../auth/auth.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnChanges {
  @Input() isEdit = false;
  @Input() producto?: ProductoResponse;
  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  // Señales principales
  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);
  activo = signal<boolean>(true);
  atributosDisponibles = signal<AtributoConValores[]>([]);

  variantes = signal<VarianteRequest[]>([]);
  collapsed = signal<boolean[]>([]);

  imagenPreviews = signal<Map<number, string>>(new Map());

  categorias = signal<DropTownStandar[]>([]);
  tiendas = signal<DropTownStandar[]>([]);
  tiendaActual = signal<DropTownStandar | null>(null);

  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService
  ) {
    this.loadDropdowns();
    this.loadAtributosConValores();
  }

  private loadDropdowns() {
    this.dropTownService.getCategorias().subscribe(cats => this.categorias.set(cats));
    if (this.auth.isAdmin()) {
      this.dropTownService.getTiendas().subscribe(tiendas => this.tiendas.set(tiendas));
    }
  }

  private loadAtributosConValores() {
    this.dropTownService.getAtributosConValores().subscribe(atributos => {
      this.atributosDisponibles.set(atributos);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['producto'] && this.producto) {
      this.nombre.set(this.producto.nombre);
      this.slug.set(this.producto.slug);
      this.categoriaId.set(this.producto.categoriaId);
      this.tiendaId.set(this.producto.tiendaId);
      this.activo.set(this.producto.activo);

      const vars = this.producto.variantes?.map((v, index) => {
        const variante: VarianteRequest = {
          id: v.id,
          sku: v.sku,
          precio: v.precio,
          stock: v.stock,
          imagenUrl: v.imagenUrl,
          activo: v.activo,
          atributos: v.atributos.map(a => ({
            atributoNombre: a.atributoNombre || '',
            valor: a.valor || ''
          }))
        };
        if (v.imagenUrl) {
          this.imagenPreviews.update(map => map.set(index, v.imagenUrl!));
        }
        return variante;
      }) || [];

      this.variantes.set(vars);
      this.collapsed.set(vars.map(() => true));

      if (!this.auth.isAdmin() && this.producto.tiendaId) {
        this.dropTownService.getTiendas().subscribe(tiendas => {
          const miTienda = tiendas.find(t => t.id === this.producto!.tiendaId);
          this.tiendaActual.set(miTienda || null);
        });
      }
    } else {
      this.resetForm();
    }
  }

  private resetForm() {
    this.nombre.set('');
    this.slug.set('');
    this.categoriaId.set(null);
    this.tiendaId.set(null);
    this.activo.set(true);
    this.variantes.set([]);
    this.collapsed.set([]);
    this.imagenPreviews.set(new Map());
    this.tiendaActual.set(null);
  }

  generarSlugDesdeNombre(): void {
    const nombreNormalizado = this.nombre().trim().toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
    this.slug.set(nombreNormalizado);
  }

  agregarVariante(): void {
    const nueva: VarianteRequest = {
      sku: '',
      precio: 0,
      stock: 0,
      activo: true,
      atributos: []
    };
    this.variantes.update(v => [...v, nueva]);
    this.collapsed.update(c => [...c, false]);
  }

  eliminarVariante(index: number): void {
    this.variantes.update(v => v.filter((_, i) => i !== index));
    this.collapsed.update(c => c.filter((_, i) => i !== index));
    this.imagenPreviews.update(map => {
      map.delete(index);
      return new Map(map);
    });
  }

  toggleCollapse(index: number): void {
    this.collapsed.update(c => c.map((val, i) => i === index ? !val : val));
  }

  // === IMÁGENES ===
  onImagenChange(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      if (!file.type.startsWith('image/')) {
        alert('Solo se permiten imágenes');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        alert('La imagen no puede superar 5MB');
        return;
      }

      this.variantes.update(v => {
        const copy = [...v];
        copy[index].imagen = file;
        copy[index].imagenUrl = undefined;
        return copy;
      });

      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagenPreviews.update(map => map.set(index, e.target?.result as string));
      };
      reader.readAsDataURL(file);
    }
  }

  eliminarImagen(index: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      delete copy[index].imagen;
      copy[index].imagenUrl = undefined;
      return copy;
    });
    this.imagenPreviews.update(map => {
      map.delete(index);
      return new Map(map);
    });
  }

  // === ATRIBUTOS ===
  agregarAtributo(varianteIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos.push({
        atributoNombre: '',
        valor: '',
        atributoNombreTemp: '',
        valorTemp: ''
      });
      return copy;
    });
  }

  eliminarAtributo(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos.splice(attrIndex, 1);
      return copy;
    });
  }

  onAtributoChange(varianteIndex: number, attrIndex: number, value: string): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex];

      if (value === '__new__') {
        attr.atributoNombre = '__new__';
        attr.atributoNombreTemp = '';
        attr.valor = '';
        attr.valorTemp = undefined;
      } else {
        attr.atributoNombre = value;
        delete (attr as any).atributoNombreTemp;
        attr.valor = '';
        attr.valorTemp = undefined;
      }
      return copy;
    });
  }

  finalizarNuevoAtributo(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex];
      const texto = attr.atributoNombreTemp?.trim();
      if (texto && texto.length > 0) {
        attr.atributoNombre = texto;
        delete (attr as any).atributoNombreTemp;
        attr.valor = '';
        attr.valorTemp = undefined;
      } else {
        attr.atributoNombre = '';
        attr.valor = '';
      }
      return copy;
    });
  }

  onValorChange(varianteIndex: number, attrIndex: number, value: string): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex];

      if (value === '__new__') {
        attr.valor = '__new__';
        attr.valorTemp = '';
      } else {
        attr.valor = value;
        delete (attr as any).valorTemp;
      }
      return copy;
    });
  }

  finalizarNuevoValor(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex];
      const texto = attr.valorTemp?.trim();
      if (texto && texto.length > 0) {
        attr.valor = texto;
        delete (attr as any).valorTemp;
      } else {
        attr.valor = '';
      }
      return copy;
    });
  }

  // ✅ MÉTODOS NUEVOS PARA EVITAR ERRORES DE COMPILACIÓN
  isAtributoPersonalizado(attr: any): boolean {
    return !!attr.atributoNombre && 
           attr.atributoNombre !== '__new__' &&
           !this.atributosDisponibles().some(a => a.descripcion === attr.atributoNombre);
  }

  isValorPersonalizado(attr: any): boolean {
    if (!attr.atributoNombre || attr.atributoNombre === '__new__' || attr.valor === '__new__') {
      return false;
    }
    const valoresExistentes = this.getValoresForAtributo(attr.atributoNombre);
    return !!attr.valor && !valoresExistentes.some(v => v.descripcion === attr.valor);
  }

  getValoresForAtributo(nombre: string): DropTownStandar[] {
    const attr = this.atributosDisponibles().find(a => a.descripcion === nombre);
    return attr ? attr.valores : [];
  }

  variantesOrdenadas = computed(() => {
    return this.variantes().map((v, i) => ({ variante: v, index: i }));
  });

  // === GUARDAR ===
  save(): void {
    this.errorMessage.set(null);
    this.loading.set(true);

    if (!this.nombre().trim() || !this.slug().trim() || !this.categoriaId()) {
      this.errorMessage.set('Completa nombre, slug y categoría.');
      this.loading.set(false);
      return;
    }

    if (this.variantes().length === 0) {
      this.errorMessage.set('Debes agregar al menos una variante.');
      this.loading.set(false);
      return;
    }

    const formData = new FormData();
    formData.append('nombre', this.nombre().trim());
    formData.append('slug', this.slug().trim());
    formData.append('categoriaId', this.categoriaId()!.toString());
    formData.append('activo', this.activo().toString());

    if (this.isEdit) {
      formData.append('tiendaId', this.producto!.tiendaId.toString());
    } else if (this.auth.isAdmin()) {
      if (!this.tiendaId()) {
        this.errorMessage.set('ADMIN: Selecciona una tienda');
        this.loading.set(false);
        return;
      }
      formData.append('tiendaId', this.tiendaId()!.toString());
    }

    this.variantes().forEach((v, i) => {
      if (v.id) formData.append(`variantes[${i}].id`, v.id.toString());
      formData.append(`variantes[${i}].sku`, v.sku.trim());
      formData.append(`variantes[${i}].precio`, v.precio.toString());
      formData.append(`variantes[${i}].stock`, v.stock.toString());
      formData.append(`variantes[${i}].activo`, (v.activo ?? true).toString());

      if (v.imagen) {
        formData.append(`variantes[${i}].imagen`, v.imagen, v.imagen.name);
      } else if (v.imagenUrl) {
        formData.append(`variantes[${i}].imagenUrl`, v.imagenUrl);
      }

      v.atributos.forEach((a, j) => {
        if (a.atributoNombre.trim() && a.valor.trim() && a.atributoNombre !== '__new__') {
          formData.append(`variantes[${i}].atributos[${j}].atributoNombre`, a.atributoNombre.trim());
          formData.append(`variantes[${i}].atributos[${j}].valor`, a.valor.trim());
        }
      });
    });

    const obs = this.isEdit
      ? this.productoService.actualizarProducto(this.producto!.id, formData)
      : this.productoService.crearProducto(formData);

    obs.subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set(err.error?.message || 'Error al guardar');
        this.loading.set(false);
      }
    });
  }

  cancel(): void {
    this.closed.emit();
  }
}