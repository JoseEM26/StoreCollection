import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AtributoConValores, ProductoRequest, ProductoResponse, VarianteRequest, AtributoValorRequest } from '../../../../model/admin/producto-admin.model';
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

  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);
  activo = signal<boolean>(true);
  atributosDisponibles = signal<AtributoConValores[]>([]);

  variantes = signal<VarianteRequest[]>([]);
  collapsed = signal<boolean[]>([]);

  // Preview de imágenes (para mostrar antes de guardar)
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
            atributoNombre: a.atributoNombre,
            valor: a.valor
          }))
        };

        // Guardar preview si ya tiene imagen
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

  // === MANEJO DE IMAGEN ===
  onImagenChange(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      // Validación básica
      if (!file.type.startsWith('image/')) {
        alert('Solo se permiten imágenes');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        alert('La imagen no puede superar 5MB');
        return;
      }

      // Guardar archivo en variante
      this.variantes.update(v => {
        const copy = [...v];
        copy[index].imagen = file;
        copy[index].imagenUrl = undefined; // Limpiar URL anterior
        return copy;
      });

      // Generar preview
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

  // === ATRIBUTOS (igual que antes) ===
  agregarAtributo(varianteIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos.push({ atributoNombre: '', valor: '' });
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

  getValoresForAtributo(nombre: string): DropTownStandar[] {
    const attr = this.atributosDisponibles().find(a => a.descripcion === nombre);
    return attr ? attr.valores : [];
  }

  variantesOrdenadas = computed(() => {
    return this.variantes().map((v, i) => ({ variante: v, index: i }));
  });

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

    // Construir FormData
    const formData = new FormData();

    // Campos básicos
    formData.append('nombre', this.nombre().trim());
    formData.append('slug', this.slug().trim());
    formData.append('categoriaId', this.categoriaId()!.toString());
    formData.append('activo', this.activo().toString());

    // TiendaId (igual lógica que antes)
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
    // OWNER: backend lo asigna automáticamente

    // Variantes
    this.variantes().forEach((v, i) => {
      if (v.id) formData.append(`variantes[${i}].id`, v.id.toString());
      formData.append(`variantes[${i}].sku`, v.sku.trim());
      formData.append(`variantes[${i}].precio`, v.precio.toString());
      formData.append(`variantes[${i}].stock`, v.stock.toString());
      formData.append(`variantes[${i}].activo`, (v.activo ?? true).toString());

      // Imagen
      if (v.imagen) {
        formData.append(`variantes[${i}].imagen`, v.imagen, v.imagen.name);
      } else if (v.imagenUrl) {
        formData.append(`variantes[${i}].imagenUrl`, v.imagenUrl);
      }

      // Atributos
      v.atributos.forEach((a, j) => {
        if (a.atributoNombre.trim() && a.valor.trim()) {
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
  onAtributoChange(varianteIndex: number, attrIndex: number, value: string): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos[attrIndex].atributoNombre = value === '__new__' ? '__new__' : value;
      copy[varianteIndex].atributos[attrIndex].valor = ''; // siempre resetear valor
      return copy;
    });
  }

  onValorChange(varianteIndex: number, attrIndex: number, value: string): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos[attrIndex].valor = value === '__new__' ? '__new__' : value;
      return copy;
    });
  }
  cancel(): void {
    this.closed.emit();
  }
}