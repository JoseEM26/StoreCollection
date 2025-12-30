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
import Swal from 'sweetalert2';

interface AtributoTemp extends AtributoValorRequest {
  atributoNombreTemp?: string;
  valorTemp?: string;
}

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
  errorMessage = signal<string | null>(null);

  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);
  activo = signal<boolean>(true);
  tiendaActual = signal<DropTownStandar | null>(null);

  categorias = signal<DropTownStandar[]>([]);
  tiendas = signal<DropTownStandar[]>([]);
  atributosDisponibles = signal<AtributoConValores[]>([]);

  variantes = signal<VarianteRequest[]>([]);
  collapsed = signal<boolean[]>([]);
  imagenPreviews = signal<Map<number, string>>(new Map());

  loading = signal<boolean>(false);

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
    if (changes['producto'] || changes['isEdit']) {
      if (this.producto && this.isEdit) {
        this.nombre.set(this.producto.nombre);
        this.slug.set(this.producto.slug);
        this.categoriaId.set(this.producto.categoriaId);
        this.tiendaId.set(this.producto.tiendaId);
        this.activo.set(this.producto.activo);

        const vars = (this.producto.variantes || []).map((v, index) => {
          const variante: VarianteRequest = {
            id: v.id,
            sku: v.sku,
            precio: v.precio,
            stock: v.stock,
            activo: v.activo,
            imagenUrl: v.imagenUrl,
            atributos: (v.atributos || []).map(a => ({
              atributoNombre: a.atributoNombre || '',
              valor: a.valor || ''
            }))
          };
          if (v.imagenUrl) {
            this.imagenPreviews.update(map => map.set(index, v.imagenUrl!));
          }
          return variante;
        });

        this.variantes.set(vars);
        this.collapsed.set(vars.map(() => true));
      } else {
        this.resetForm();
      }
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
  }

  generarSlugDesdeNombre(): void {
    const slug = this.nombre()
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');

    this.slug.set(slug);
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
      return map;
    });
  }

  toggleCollapse(index: number): void {
    this.collapsed.update(c => c.map((val, i) => i === index ? !val : val));
  }

  onImagenChange(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;

    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      Swal.fire('Archivo inválido', 'Solo imágenes permitidas', 'warning');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      Swal.fire('Imagen muy grande', 'Máximo 5 MB', 'warning');
      return;
    }

    this.variantes.update(v => {
      const copy = [...v];
      copy[index].imagen = file;
      copy[index].imagenUrl = undefined;
      return copy;
    });

    const reader = new FileReader();
    reader.onload = () => {
      this.imagenPreviews.update(map => map.set(index, reader.result as string));
    };
    reader.readAsDataURL(file);
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
      return map;
    });
  }

  // ======================== ATRIBUTOS (métodos requeridos por tu HTML) ========================

  agregarAtributo(varianteIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos.push({
        atributoNombre: '',
        valor: ''
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
      const attr = copy[varianteIndex].atributos[attrIndex] as AtributoTemp;

      if (value === '__new__') {
        attr.atributoNombre = '__new__';
        attr.atributoNombreTemp = '';
      } else {
        attr.atributoNombre = value;
        delete attr.atributoNombreTemp;
      }
      attr.valor = '';
      return copy;
    });
  }

  finalizarNuevoAtributo(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex] as AtributoTemp;
      const nuevoNombre = attr.atributoNombreTemp?.trim();

      if (nuevoNombre) {
        attr.atributoNombre = nuevoNombre;
      } else {
        attr.atributoNombre = '';
      }
      delete attr.atributoNombreTemp;
      attr.valor = '';
      return copy;
    });
  }

  onValorChange(varianteIndex: number, attrIndex: number, value: string): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex] as AtributoTemp;

      if (value === '__new__') {
        attr.valor = '__new__';
        attr.valorTemp = '';
      } else {
        attr.valor = value;
        delete attr.valorTemp;
      }
      return copy;
    });
  }

  finalizarNuevoValor(varianteIndex: number, attrIndex: number): void {
    this.variantes.update(v => {
      const copy = [...v];
      const attr = copy[varianteIndex].atributos[attrIndex] as AtributoTemp;
      const nuevoValor = attr.valorTemp?.trim();

      if (nuevoValor) {
        attr.valor = nuevoValor;
      } else {
        attr.valor = '';
      }
      delete attr.valorTemp;
      return copy;
    });
  }

  getValoresForAtributo(nombre: string): DropTownStandar[] {
    const attr = this.atributosDisponibles().find(a => a.descripcion === nombre);
    return attr?.valores || [];
  }
  // ======================== GUARDAR ========================

  save(): void {
    this.errorMessage.set(null);
    this.loading.set(true);

    // Validaciones previas
    if (!this.nombre().trim()) {
      this.mostrarAlerta('warning', 'Nombre requerido', 'El nombre del producto es obligatorio.');
      return;
    }

    if (!this.slug().trim()) {
      this.mostrarAlerta('warning', 'Slug requerido', 'El slug es necesario para la URL del producto. Puedes generarlo desde el nombre.');
      return;
    }

    if (!/^[a-z0-9-]+$/.test(this.slug().trim())) {
      this.mostrarAlerta('warning', 'Slug inválido', 'Solo letras minúsculas, números y guiones (-). Ej: camiseta-nike-2025');
      return;
    }

    if (!this.categoriaId()) {
      this.mostrarAlerta('warning', 'Categoría obligatoria', 'Selecciona una categoría para organizar el producto.');
      return;
    }

    if (this.variantes().length === 0) {
      this.mostrarAlerta('warning', 'Faltan variantes', 'Todo producto necesita al menos una variante con precio y SKU.');
      return;
    }

    // Validar cada variante
    for (let i = 0; i < this.variantes().length; i++) {
      const v = this.variantes()[i];

      if (!v.sku?.trim()) {
        this.mostrarAlerta('warning', `Variante ${i + 1}: SKU vacío`, 'Cada variante debe tener un código único (ej: CAM-ROJ-40).');
        return;
      }

      if (!v.precio || v.precio <= 0) {
        this.mostrarAlerta('warning', `Variante ${i + 1}: Precio inválido`, 'El precio debe ser mayor a 0.');
        return;
      }

      if (v.stock == null || v.stock < 0) {
        this.mostrarAlerta('warning', `Variante ${i + 1}: Stock inválido`, 'El stock no puede ser negativo.');
        return;
      }

      for (let j = 0; j < v.atributos.length; j++) {
        const a = v.atributos[j];
        if ((a.atributoNombre?.trim() && !a.valor?.trim()) || (!a.atributoNombre?.trim() && a.valor?.trim())) {
          this.mostrarAlerta('warning', `Variante ${i + 1}: Atributo incompleto`, 'Completa tanto el atributo como su valor.');
          return;
        }
      }
    }

    this.enviarFormulario();
  }
  variantesOrdenadas = computed(() => {
    return this.variantes().map((v, i) => ({ variante: v, index: i }));
  });
  private enviarFormulario(): void {
    const formData = new FormData();

    formData.append('nombre', this.nombre().trim());
    formData.append('slug', this.slug().trim());
    formData.append('categoriaId', this.categoriaId()!.toString());
    formData.append('activo', this.activo().toString());

    // Tienda: solo admin la envía
    if (this.auth.isAdmin()) {
      formData.append('tiendaId', this.tiendaId()!.toString());
    }

    // Variantes
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
        if (a.atributoNombre?.trim() && a.valor?.trim()) {
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
        Swal.fire({
          icon: 'success',
          title: '¡Éxito!',
          text: this.isEdit ? 'Producto actualizado correctamente' : 'Producto creado exitosamente',
          timer: 2000,
          showConfirmButton: false
        });
        this.saved.emit();
      },
      error: (err) => {
        this.loading.set(false);
        const mensaje = err.error?.message || 'Error al guardar el producto. Revisa los datos.';
        Swal.fire({
          icon: 'error',
          title: 'Error al guardar',
          text: mensaje,
          confirmButtonText: 'Entendido'
        });
      }
    });
  }

  private mostrarAlerta(icon: 'warning' | 'error' | 'info', title: string, text: string) {
    this.loading.set(false);
    Swal.fire({
      icon,
      title,
      text,
      confirmButtonText: 'Corregir',
      confirmButtonColor: '#3085d6'
    });
  }

  cancel(): void {
    this.closed.emit();
  }
}