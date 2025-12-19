import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoRequest, ProductoResponse, VarianteRequest } from '../../../../model/admin/producto-admin.model';
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

  // Señales del formulario
  nombre = signal<string>('');
  slug = signal<string>('');
  categoriaId = signal<number | null>(null);
  tiendaId = signal<number | null>(null);
  activo = signal<boolean>(true);
atributosDisponibles = signal<DropTownStandar[]>([]);

  // Variantes
  variantes = signal<VarianteRequest[]>([]);
  collapsed = signal<boolean[]>([]);

  // Dropdowns
  categorias = signal<DropTownStandar[]>([]);
  tiendas = signal<DropTownStandar[]>([]);

  // Tienda actual del owner (solo lectura)
  tiendaActual = signal<DropTownStandar | null>(null);

  // Estados
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private productoService: ProductoAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService
  ) {
    this.loadAtributos();
  }
private loadAtributos() {
  this.dropTownService.getAtributos().subscribe(atributos => {
    this.atributosDisponibles.set(atributos);
    console.log('Atributos disponibles cargados:', atributos);
  });
}
agregarAtributoDesdeDropdown(varianteIndex: number, atributoId: number | null) {
  if (!atributoId) return;

  const atributo = this.atributosDisponibles().find(a => a.id === atributoId);
  if (atributo) {
    this.variantes.update(v => {
      const copy = [...v];
      copy[varianteIndex].atributos.push({
        atributoNombre: atributo.descripcion,
        valor: ''  // El usuario completa el valor (ej: "Rojo", "38")
      });
      return copy;
    });
  }
}
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['producto'] && this.producto) {
      console.log('Producto recibido para edición:', this.producto);

      this.nombre.set(this.producto.nombre);
      this.slug.set(this.producto.slug);
      this.categoriaId.set(this.producto.categoriaId);

      // CLAVE: Siempre seteamos tiendaId desde el producto original (igual que en categorías)
      this.tiendaId.set(this.producto.tiendaId);
      console.log('tiendaId seteado desde producto:', this.producto.tiendaId);

      this.activo.set(this.producto.activo);

      // Mapeo de variantes
      const vars = this.producto.variantes?.map(v => ({
        id: v.id,
        sku: v.sku,
        precio: v.precio,
        stock: v.stock,
        imagenUrl: v.imagenUrl ?? '',
        activo: v.activo,
        atributos: v.atributos.map(a => ({
          atributoNombre: a.atributoNombre,
          valor: a.valor
        }))
      })) || [];

      this.variantes.set(vars);
      this.collapsed.set(vars.map(() => true));

      // Cargar nombre de tienda para OWNER (solo lectura)
      if (!this.auth.isAdmin() && this.producto.tiendaId) {
        this.dropTownService.getTiendas().subscribe(tiendas => {
          const miTienda = tiendas.find(t => t.id === this.producto!.tiendaId);
          this.tiendaActual.set(miTienda || null);
          console.log('Tienda del OWNER cargada:', miTienda);
        });
      }
    } else {
      // Modo creación: limpiar todo
      this.nombre.set('');
      this.slug.set('');
      this.categoriaId.set(null);
      this.tiendaId.set(null);
      this.activo.set(true);
      this.variantes.set([]);
      this.collapsed.set([]);
      this.tiendaActual.set(null);
    }

    this.loadDropdowns();
  }

  private loadDropdowns() {
    this.dropTownService.getCategorias().subscribe(cats => {
      this.categorias.set(cats);
      console.log('Categorías cargadas:', cats.length);
    });

    if (this.auth.isAdmin()) {
      this.dropTownService.getTiendas().subscribe(tiendas => {
        this.tiendas.set(tiendas);
        console.log('Tiendas cargadas para ADMIN:', tiendas);
      });
    }
  }

  // TrackBy
  trackByVarianteIndex(index: number, item: { variante: VarianteRequest; index: number }): any {
    return item.variante.id || index;
  }

  trackByAtributoIndex(index: number): any {
    return index;
  }

  generarSlugDesdeNombre(): void {
    const nombreNormalizado = this.nombre().trim().toLowerCase()
      .replace(/á/g, 'a').replace(/é/g, 'e').replace(/í/g, 'i')
      .replace(/ó/g, 'o').replace(/ú/g, 'u').replace(/ñ/g, 'n')
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
      imagenUrl: '',
      activo: true,
      atributos: []
    };
    this.variantes.update(v => [...v, nueva]);
    this.collapsed.update(c => [...c, false]);
  }

  eliminarVariante(index: number): void {
    this.variantes.update(v => v.filter((_, i) => i !== index));
    this.collapsed.update(c => c.filter((_, i) => i !== index));
  }

  toggleCollapse(index: number): void {
    this.collapsed.update(c => c.map((val, i) => i === index ? !val : val));
  }

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

  const request: ProductoRequest = {
    nombre: this.nombre().trim(),
    slug: this.slug().trim(),
    categoriaId: this.categoriaId()!,
    activo: this.activo(),
    variantes: this.variantes().map(v => ({
      id: v.id,
      sku: v.sku.trim(),
      precio: v.precio,
      stock: v.stock,
      imagenUrl: v.imagenUrl?.trim() || undefined,
      activo: v.activo,
      atributos: v.atributos.filter(a => a.atributoNombre.trim() && a.valor.trim())
    }))
  };

  // =================== LÓGICA DE TIENDAID ===================

  // 1. Si es EDICIÓN → siempre enviar la tienda del producto original
  if (this.isEdit) {
    request.tiendaId = this.producto!.tiendaId;
    console.log('EDICIÓN → tiendaId enviado:', request.tiendaId);
    this.enviarRequest(request);
    return;
  }

  // 2. Si es ADMIN → usar el dropdown seleccionado
  if (this.auth.isAdmin()) {
    if (!this.tiendaId()) {
      this.errorMessage.set('ADMIN: Debes seleccionar una tienda.');
      this.loading.set(false);
      return;
    }
    request.tiendaId = this.tiendaId()!;
    console.log('ADMIN CREACIÓN → tiendaId seleccionado:', request.tiendaId);
    this.enviarRequest(request);
    return;
  }

  // 3. Si es OWNER y es CREACIÓN → obtener automáticamente su tienda
  // (tu backend ya filtra y devuelve solo su tienda)
  this.dropTownService.getTiendas().subscribe({
    next: (tiendas) => {
      if (tiendas.length === 0) {
        this.errorMessage.set('No tienes ninguna tienda asignada.');
        this.loading.set(false);
        return;
      }
      // OWNER solo tiene una tienda → tomamos la primera (o única)
      request.tiendaId = tiendas[0].id;
      console.log('OWNER CREACIÓN → tiendaId automático:', request.tiendaId);

      this.enviarRequest(request);
    },
    error: (err) => {
      console.error('Error al cargar tiendas del OWNER', err);
      this.errorMessage.set('No se pudo obtener tu tienda.');
      this.loading.set(false);
    }
  });
}

// Método auxiliar para enviar la petición
private enviarRequest(request: ProductoRequest) {
  console.log('REQUEST FINAL enviado al backend:', {
    ...request,
    tiendaId: request.tiendaId ?? 'ERROR: undefined',
    isEdit: this.isEdit,
    isAdmin: this.auth.isAdmin()
  });

  const obs = this.isEdit
    ? this.productoService.actualizarProducto(this.producto!.id, request)
    : this.productoService.crearProducto(request);

  obs.subscribe({
    next: (res) => {
      console.log('Producto guardado exitosamente:', res);
      this.loading.set(false);
      this.saved.emit();
    },
    error: (err) => {
      console.error('Error del backend:', err);
      this.errorMessage.set(err.error?.message || 'Error al guardar el producto');
      this.loading.set(false);
    }
  });
}

  cancel(): void {
    console.log('Formulario cancelado');
    this.closed.emit();
  }
}