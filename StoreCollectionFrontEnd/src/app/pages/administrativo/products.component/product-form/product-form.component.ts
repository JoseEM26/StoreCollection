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
descripcionCortaProducto = signal<string | null>(null);
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
  // Siempre reseteamos primero (garantiza formulario limpio en modo creación)
  this.resetForm();

  // Solo si estamos realmente en modo edición y hay producto → cargamos datos
  if (this.isEdit && this.producto) {
    this.nombre.set(this.producto.nombre || '');
    this.slug.set(this.producto.slug || '');
    this.categoriaId.set(this.producto.categoriaId ?? null);
    this.tiendaId.set(this.producto.tiendaId ?? null);
    this.activo.set(this.producto.activo ?? true);
    const variantesMapeadas = this.producto.variantes?.map((v, index) => {
      const variante: VarianteRequest = {
        id: v.id,
        sku: v.sku || '',
        precio: v.precio || 0,
        stock: v.stock ?? 0,
        imagenUrl: v.imagenUrl,
        activo: v.activo ?? true,
        precio_anterior: v.precio_anterior ?? null,          // ← NUEVO
        descripcion_corta: v.descripcion_corta ?? null,      // ← NUEVO
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

    this.variantes.set(variantesMapeadas);
    this.collapsed.set(variantesMapeadas.map(() => true));

    if (!this.auth.isAdmin() && this.producto.tiendaId) {
      this.dropTownService.getTiendas().subscribe(tiendas => {
        const miTienda = tiendas.find(t => t.id === this.producto!.tiendaId);
        this.tiendaActual.set(miTienda || null);
      });
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
    this.tiendaActual.set(null);
    this.descripcionCortaProducto.set(null);
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
    precio_anterior: null,               // ← NUEVO (opcional)
    descripcion_corta: null,             // ← NUEVO (opcional)
      atributos: []
    };
    this.variantes.update(v => [...v, nueva]);
    this.collapsed.update(c => [...c, false]);
  }
// Devuelve true si el atributo ya está seleccionado en esta variante
isAtributoYaUsado(varianteIndex: number, atributoNombre: string, excludeIndex?: number): boolean {
  const variante = this.variantes()[varianteIndex];
  return variante.atributos.some((attr, idx) => {
    // Ignoramos el índice actual si estamos editando uno existente
    if (excludeIndex !== undefined && idx === excludeIndex) return false;
    return attr.atributoNombre?.trim().toLowerCase() === atributoNombre?.trim().toLowerCase();
  });
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

  onImagenChange(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      if (!file.type.startsWith('image/')) {
        Swal.fire({
          icon: 'warning',
          title: 'Archivo no permitido',
          text: 'Solo se aceptan imágenes.',
          confirmButtonText: 'Entendido'
        });
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        Swal.fire({
          icon: 'warning',
          title: 'Imagen demasiado grande',
          text: 'La imagen no puede superar los 5 MB.',
          confirmButtonText: 'OK'
        });
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
    return map;
  });
}
agregarAtributo(varianteIndex: number): void {
  const variante = this.variantes()[varianteIndex];

  // Creamos un Set con los nombres de atributos ya usados (normalizados a minúsculas)
  const nombresUsados = new Set(
    variante.atributos
      .map(a => a.atributoNombre?.trim().toLowerCase())
      .filter(nombre => nombre && nombre !== '__new__')
  );

  // 1. Comprobamos si ya se usaron todos los atributos disponibles
  if (nombresUsados.size >= this.atributosDisponibles().length) {
    Swal.fire({
      icon: 'info',
      title: 'No quedan atributos disponibles',
      text: 'Ya has usado todos los tipos de atributos posibles en esta variante.',
      timer: 3000,
      showConfirmButton: false
    });
    return;
  }

  // 2. Comprobación extra (más amigable): si no hay ninguno disponible sin usar
  const hayDisponibles = this.atributosDisponibles().some(
    attr => !nombresUsados.has(attr.descripcion.trim().toLowerCase())
  );

  if (!hayDisponibles) {
    Swal.fire({
      icon: 'info',
      title: 'Sin atributos disponibles',
      text: 'Ya has usado todos los atributos posibles en esta variante.',
      confirmButtonText: 'OK'
    });
    return;
  }

  // Todo está bien → agregamos una nueva fila de atributo
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

  // Expandimos la variante para que el usuario vea inmediatamente el nuevo campo
  this.collapsed.update(c => c.map((val, i) => i === varianteIndex ? false : val));
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

    // ¡¡ Actualizamos SKU automáticamente !!
    copy[varianteIndex].sku = this.generarSkuSugerido(varianteIndex);

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

    // Actualizamos SKU también al finalizar valor personalizado
    copy[varianteIndex].sku = this.generarSkuSugerido(varianteIndex);

    return copy;
  });
}
private skuEstaDuplicado(sku: string, excludeIndex: number): boolean {
  return this.variantes().some((v, i) => 
    i !== excludeIndex && 
    v.sku?.trim().toUpperCase() === sku.trim().toUpperCase()
  );
}
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

save(): void {
  this.errorMessage.set(null);
  this.loading.set(true);

  // ── Validaciones generales del producto ──────────────────────────────────────
  if (!this.nombre().trim()) {
    this.mostrarAlertaValidacion(
      'warning',
      'Falta el nombre del producto',
      'El nombre es obligatorio para identificar el producto en la tienda.'
    );
    return;
  }

  const slugTrim = this.slug().trim();
  if (!slugTrim) {
    this.mostrarAlertaValidacion(
      'warning',
      'Slug requerido',
      'El slug forma parte de la URL del producto.<br>Puedes generarlo automáticamente desde el nombre.'
    );
    return;
  }

  if (!/^[a-z0-9-]+$/.test(slugTrim)) {
    this.mostrarAlertaValidacion(
      'warning',
      'Formato de slug inválido',
      'Solo se permiten letras minúsculas, números y guiones (-).<br>Ejemplo válido: <b>zapatillas-nike-air-2024</b>'
    );
    return;
  }

  if (!this.categoriaId()) {
    this.mostrarAlertaValidacion(
      'warning',
      'Selecciona una categoría',
      'La categoría ayuda a organizar y mostrar el producto correctamente.'
    );
    return;
  }

  if (!this.isEdit && this.auth.isAdmin() && !this.tiendaId()) {
    this.mostrarAlertaValidacion(
      'warning',
      'Tienda no seleccionada',
      'Como administrador, debes asignar este producto a una tienda específica.'
    );
    return;
  }

  if (this.variantes().length === 0) {
    this.mostrarAlertaValidacion(
      'warning',
      'No hay variantes',
      'Todo producto necesita al menos una variante con precio, stock y SKU.'
    );
    return;
  }

  // ── Validaciones por cada variante ───────────────────────────────────────────
  for (let i = 0; i < this.variantes().length; i++) {
    const v = this.variantes()[i];
    const numVariante = i + 1;

    // 1. SKU
    let skuFinal = v.sku?.trim() || '';

    if (!skuFinal) {
      // Auto-generamos si está vacío
      skuFinal = this.generarSkuSugerido(i);
      this.variantes.update(variantes => {
        const copy = [...variantes];
        copy[i].sku = skuFinal;
        return copy;
      });

      Swal.fire({
        icon: 'info',
        title: `SKU generado automáticamente - Variante ${numVariante}`,
        html: `Se ha asignado el siguiente SKU único:<br><b>${skuFinal}</b><br><br>Puedes modificarlo si lo deseas.`,
        timer: 3500,
        timerProgressBar: true,
        showConfirmButton: false,
        toast: true,
        position: 'top-end',
        background: '#e3f2fd',
        color: '#1e88e5'
      });
    }

    // Verificamos duplicados después de generar
    if (this.skuEstaDuplicado(skuFinal, i)) {
      this.mostrarAlertaValidacion(
        'error',
        `SKU duplicado - Variante ${numVariante}`,
        `El SKU <b>"${skuFinal}"</b> ya está siendo usado en otra variante.<br>Cada variante debe tener un SKU único.`
      );
      return;
    }

    // 2. Precio anterior (si existe)
    if (v.precio_anterior != null && v.precio_anterior <= v.precio) {
      this.mostrarAlertaValidacion(
        'warning',
        `Precio anterior inválido - Variante ${numVariante}`,
        'El precio anterior debe ser **mayor** al precio actual para mostrar la oferta correctamente.'
      );
      return;
    }

    // 3. Precio
    if (!v.precio || v.precio <= 0) {
      this.mostrarAlertaValidacion(
        'warning',
        `Precio inválido - Variante ${numVariante}`,
        'El precio debe ser mayor que cero.'
      );
      return;
    }

    // 4. Stock
    if (v.stock == null || v.stock < 0) {
      this.mostrarAlertaValidacion(
        'warning',
        `Stock inválido - Variante ${numVariante}`,
        'El stock no puede ser negativo. Usa 0 si no hay disponibilidad.'
      );
      return;
    }

    // 5. Atributos completos
    for (let j = 0; j < v.atributos.length; j++) {
      const a = v.atributos[j];
      if (a.atributoNombre && !a.valor?.trim()) {
        this.mostrarAlertaValidacion(
          'warning',
          `Valor de atributo faltante - Variante ${numVariante}`,
          `Has seleccionado el atributo "<b>${a.atributoNombre}</b>" pero no has indicado su valor.`
        );
        return;
      }
      if (!a.atributoNombre?.trim() && a.valor?.trim()) {
        this.mostrarAlertaValidacion(
          'warning',
          `Atributo incompleto - Variante ${numVariante}`,
          'Has indicado un valor pero no has seleccionado o escrito el nombre del atributo.'
        );
        return;
      }
    }
  }

  // ── Última validación: atributos duplicados por variante ─────────────────────
  for (let i = 0; i < this.variantes().length; i++) {
    const v = this.variantes()[i];
    const nombresAtributos = v.atributos
      .map(a => a.atributoNombre?.trim().toLowerCase())
      .filter(Boolean) as string[];

    const duplicados = nombresAtributos.filter(
      (nombre, idx) => nombresAtributos.indexOf(nombre) !== idx
    );

    if (duplicados.length > 0) {
      this.mostrarAlertaValidacion(
        'error',
        'Atributos duplicados',
        `La variante ${i + 1} tiene atributos repetidos: <b>${duplicados.join(', ')}</b>.<br>Cada atributo debe ser único por variante.`
      );
      return;
    }
  }

  // ── Todo OK → guardar ───────────────────────────────────────────────────────
  this.enviarFormulario();
}

// Método auxiliar para alertas consistentes y bonitas
private mostrarAlertaValidacion(icon: 'warning' | 'error' | 'info', title: string, html: string): void {
  Swal.fire({
    icon,
    title,
    html,
    confirmButtonText: icon === 'error' ? 'Corregir ahora' : 'Entendido',
    confirmButtonColor: icon === 'error' ? '#ef4444' : '#3b82f6',
    allowOutsideClick: false,
    allowEscapeKey: false,
    showClass: { popup: 'animate__animated animate__fadeInDown animate__faster' },
    hideClass: { popup: 'animate__animated animate__fadeOutUp animate__faster' }
  });
  this.loading.set(false);
}

  private enviarFormulario(): void {
    const formData = new FormData();

    formData.append('nombre', this.nombre().trim());
    formData.append('slug', this.slug().trim());
    formData.append('categoriaId', this.categoriaId()!.toString());
    formData.append('activo', this.activo().toString());
formData.append('descripcion_corta', this.descripcionCortaProducto()?.trim() || '');
    if (this.isEdit) {
      formData.append('tiendaId', this.producto!.tiendaId.toString());
    } else if (this.auth.isAdmin()) {
      formData.append('tiendaId', this.tiendaId()!.toString());
    }

    this.variantes().forEach((v, i) => {
      if (v.id) formData.append(`variantes[${i}].id`, v.id.toString());
      formData.append(`variantes[${i}].sku`, v.sku.trim());
      formData.append(`variantes[${i}].precio`, v.precio.toString());
      formData.append(`variantes[${i}].stock`, v.stock.toString());
      formData.append(`variantes[${i}].activo`, (v.activo ?? true).toString());
// ¡¡ CAMPOS NUEVOS - AGREGAR AQUÍ !!
    if (v.precio_anterior !== null && v.precio_anterior !== undefined) {
      formData.append(`variantes[${i}].precio_anterior`, v.precio_anterior.toString());
    }
    if (v.descripcion_corta?.trim()) {
      formData.append(`variantes[${i}].descripcion_corta`, v.descripcion_corta.trim());
    }
      if (v.imagen) {
        formData.append(`variantes[${i}].imagen`, v.imagen, v.imagen.name);
      } else if (v.imagenUrl) {
        formData.append(`variantes[${i}].imagenUrl`, v.imagenUrl);
      }

      v.atributos.forEach((a, j) => {
        if (a.atributoNombre?.trim() && a.valor?.trim() && a.atributoNombre !== '__new__') {
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
          title: '¡Guardado!',
          text: this.isEdit ? 'Producto actualizado correctamente.' : 'Producto creado correctamente.',
          timer: 2000,
          showConfirmButton: false
        });
        this.saved.emit();
      },
      error: (err) => {
        console.error(err);
        const mensaje = err.error?.message || 'Error al guardar el producto.';
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: mensaje,
          confirmButtonText: 'OK'
        });
        this.loading.set(false);
      }
    });
  }
private generarSkuSugerido(varianteIndex: number): string {
  // 1. Tomamos una versión corta y limpia del nombre del producto
  const nombreBase = this.nombre()
    .trim()
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .substring(0, 12); // ← límite razonable (ej: ZAPATILLAS-NIKE)

  // 2. Obtenemos los valores de atributos de esta variante
  const variante = this.variantes()[varianteIndex];
  const valores = variante.atributos
    .filter(a => a.valor?.trim() && a.atributoNombre !== '__new__')
    .map(a => a.valor!.trim().toUpperCase()
      .replace(/\s+/g, '')
      .substring(0, 8) // ← evitamos valores muy largos
    )
    .filter(Boolean);

  // 3. Construimos el SKU
  let sku = nombreBase;
  if (valores.length > 0) {
    sku += '-' + valores.join('-');
  }

  // 4. Limpiamos caracteres no deseados y quitamos dobles guiones
  sku = sku
    .replace(/[^A-Z0-9-]/g, '')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');

  return sku;
}
  cancel(): void {
    this.closed.emit();
  }
}