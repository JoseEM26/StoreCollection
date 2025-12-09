// src/app/pages/administrativo/products/product-form/product-form.component.ts
import { Component, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { ProductoAdminService } from '../../../../service/service-admin/producto-admin.service';
import { CategoriaDropdown } from '../../../../model/admin/categoria-admin.model';
import { AtributoConValores, AtributoValorDto } from '../../../../model/admin/atributo-dropdown.model';
import { ProductoResponse, VarianteResponse } from '../../../../model/admin/producto-admin.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent {
  productId = input<number | null>(null);
  isEdit = input<boolean>(false);
  close = output<void>();
  saved = output<void>();

  categorias = signal<CategoriaDropdown[]>([]);
  atributos = signal<AtributoConValores[]>([]);

  loading = signal(true);
  saving = signal(false);

  form!: FormGroup;
  private fb = new FormBuilder();
  private variantesArray = this.fb.array<FormGroup>([]);

  constructor(private productoService: ProductoAdminService) {
    this.inicializarFormulario();
    this.cargarDatosIniciales();
  }

  private inicializarFormulario() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      categoriaId: [null, Validators.required],
      variantes: this.variantesArray
    });
  }

  private cargarDatosIniciales() {
    Promise.all([
      this.productoService.obtenerCategorias().toPromise().then(r => r || []),
      this.productoService.obtenerAtributos().toPromise().then(r => r || [])
    ]).then(([cats, attrs]) => {
      this.categorias.set(cats);
      this.atributos.set(attrs);

      // Agregar primera variante vacía (con atributos correctos)
      this.agregarVarianteVacia();

      // Si es edición → cargar producto
      if (this.productId() !== null && this.isEdit()) {
        this.cargarProducto(this.productId()!);
      } else {
        this.loading.set(false);
      }
    }).catch(err => {
      console.error('Error cargando datos', err);
      alert('Error al cargar categorías o atributos');
      this.loading.set(false);
    });
  }

  private cargarProducto(id: number) {
    this.productoService.obtenerProducto(id).subscribe({
      next: (prod) => {
        this.form.patchValue({
          nombre: prod.nombre,
          slug: prod.slug,
          categoriaId: prod.categoriaId
        });

        this.variantesArray.clear();
        prod.variantes.forEach(v => this.agregarVarianteDesdeResponse(v));
        if (prod.variantes.length === 0) this.agregarVarianteVacia();

        this.loading.set(false);
      },
      error: () => {
        alert('Error al cargar el producto');
        this.loading.set(false);
      }
    });
  }

  // Variante vacía
  private agregarVarianteVacia() {
    const grupo = this.fb.group({
      id: [null],
      sku: [this.generarSkuSugerido(), [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/)]],
      precio: [0, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.min(0)]],
      imagenUrl: [''],
      activo: [true],
      atributosSeleccionados: this.crearGrupoAtributos({})
    });
    this.variantesArray.push(grupo);
  }

  // Variante al editar
  private agregarVarianteDesdeResponse(variante: VarianteResponse) {
    const attrs: Record<string, string> = {};
    variante.atributos.forEach(a => attrs[a.nombreAtributo] = a.valor);

    const grupo = this.fb.group({
      id: [variante.id],
      sku: [variante.sku, [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/)]],
      precio: [variante.precio, [Validators.required, Validators.min(0.01)]],
      stock: [variante.stock ?? 0, [Validators.min(0)]],
      imagenUrl: [variante.imagenUrl || ''],
      activo: [variante.activo ?? true],
      atributosSeleccionados: this.crearGrupoAtributos(attrs)
    });
    this.variantesArray.push(grupo);
  }

  // Crea el grupo de atributos dinámicos
  private crearGrupoAtributos(valoresIniciales: Record<string, string>): FormGroup {
    const controles: Record<string, any> = {};
    this.atributos().forEach(attr => {
      controles[attr.nombre] = [valoresIniciales[attr.nombre] || ''];
    });
    return this.fb.group(controles);
  }

  private generarSkuSugerido(): string {
    const base = (this.form.get('slug')?.value || 'prod').toUpperCase().replace(/[^A-Z0-9]/g, '');
    return `${base}-${String(this.variantesArray.length + 1).padStart(3, '0')}`;
  }

  generateSlug() {
    const nombre = this.form.get('nombre')?.value?.trim();
    if (nombre && !this.form.get('slug')?.dirty) {
      const slug = nombre.toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-z0-9\s-]/g, '')
        .trim()
        .replace(/\s+/g, '-');
      this.form.get('slug')?.setValue(slug);
    }
  }

  agregarVariante() {
    this.agregarVarianteVacia();
  }

  eliminarVariante(index: number) {
    if (this.variantesArray.length > 1) {
      this.variantesArray.removeAt(index);
    }
  }

  guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      alert('Corrige los errores antes de guardar');
      return;
    }

    const raw = this.form.getRawValue();
    const request = {
      id: this.productId() || undefined,
      nombre: raw.nombre.trim(),
      slug: raw.slug.trim(),
      categoriaId: raw.categoriaId,
      variantes: raw.variantes.map((v: any) => ({
        id: v.id,
        sku: v.sku.trim(),
        precio: Number(v.precio),
        stock: Number(v.stock) || 0,
        imagenUrl: v.imagenUrl.trim() || undefined,
        activo: v.activo,
        atributoValorIds: this.convertirAtributosSeleccionados(v.atributosSeleccionados)
      }))
    };

    this.saving.set(true);
    const obs = this.isEdit() && this.productId()
      ? this.productoService.actualizarProducto(this.productId()!, request)
      : this.productoService.crearProducto(request);

    obs.subscribe({
      next: () => {
        alert(`Producto ${this.isEdit() ? 'actualizado' : 'creado'} con éxito`);
        this.saved.emit();
        this.close.emit();
      },
      error: (err) => {
        alert(err.error?.message || 'Error al guardar');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private convertirAtributosSeleccionados(seleccionados: Record<string, string>): number[] {
    const ids: number[] = [];
    for (const [nombre, valor] of Object.entries(seleccionados)) {
      if (!valor) continue;
      const attr = this.atributos().find(a => a.nombre === nombre);
      const val = attr?.valores.find(v => v.valor === valor);
      if (val) ids.push(val.id);
    }
    return ids;
  }

  get variantes() {
    return this.variantesArray as FormArray;
  }

  getValoresDe(nombre: string) {
    return this.atributos().find(a => a.nombre === nombre)?.valores || [];
  }

  getError(campo: string, index?: number): string {
    let control;
    if (index !== undefined) {
      const variante = this.variantes.at(index) as FormGroup;
      control = variante.get(campo);
    } else {
      control = this.form.get(campo);
    }
    if (!control?.touched) return '';
    if (control.hasError('required')) return 'Obligatorio';
    if (control.hasError('minlength')) return `Mínimo ${control.errors?.['minlength'].requiredLength} caracteres`;
    if (control.hasError('pattern')) return campo === 'slug' ? 'Solo letras, números y guiones' : 'Formato inválido';
    if (control.hasError('min')) return 'Debe ser mayor a 0';
    return 'Inválido';
  }
}