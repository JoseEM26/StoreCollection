// src/app/pages/admin/stores/form-stores/form-stores.component.ts
import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';



import { lastValueFrom } from 'rxjs';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';
import { AuthService } from '../../../../../auth/auth.service';
import { TiendaResponse } from '../../../../model/admin/tienda-admin.model';
import { TiendaAdminService, TiendaCreateRequest, TiendaUpdateRequest } from '../../../../service/service-admin/tienda-admin.service';

@Component({
  selector: 'app-form-stores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './form-stores.component.html',
  styleUrl: './form-stores.component.css'
})
export class FormStoresComponent implements OnInit, OnChanges {
  @Input() tienda?: TiendaResponse;
  @Output() success = new EventEmitter<TiendaResponse>();
  @Output() cancel = new EventEmitter<void>();

  isEditMode = false;
  loading = false;
  logoPreview: string | null = null;
  selectedFile: File | null = null;
  serverError: string | null = null;
  fileError: string | null = null;

  usuarios: DropTownStandar[] = [];
  usuariosLoading = false;

  planes: DropTownStandar[] = [];
  planesLoading = false;

  esAdmin = false;

  form = new FormGroup({
    nombre: new FormControl<string>('', [Validators.required, Validators.minLength(3)]),
    slug: new FormControl<string>('', [
      Validators.required,
      Validators.pattern(/^[a-z0-9-]+$/),
      Validators.maxLength(50)
    ]),
    whatsapp: new FormControl<string>('+51', [
      Validators.pattern(/^(\+51\s?)?[0-9]{9,15}$/)
    ]),
    moneda: new FormControl<'SOLES' | 'DOLARES'>('SOLES', [Validators.required]),
    descripcion: new FormControl<string>(''),
    direccion: new FormControl<string>(''),
    horarios: new FormControl<string>('Lun - Sáb 9:00 - 21:00'),
    mapa_url: new FormControl<string>('', [Validators.pattern(/^https?:\/\/.+/)]),
    userId: new FormControl<number | null>(null),
    planId: new FormControl<number | null>(null),
    activo: new FormControl<boolean>(true)
  });

  constructor(
    private tiendaService: TiendaAdminService,
    private dropTownService: DropTownService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.auth.isAdmin();
    this.configurarValidadores();

    this.form.get('slug')?.enable();

    if (this.esAdmin) {
      this.cargarUsuarios();
      this.cargarPlanes();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tienda'] && changes['tienda'].currentValue !== changes['tienda'].previousValue) {
      if (this.tienda) {
        // === MODO EDICIÓN ===
        this.isEditMode = true;
        this.serverError = null;
        this.logoPreview = this.tienda.logo_img_url || null;
        this.selectedFile = null;

        this.form.patchValue({
          nombre: this.tienda.nombre,
          slug: this.tienda.slug,
          whatsapp: this.tienda.whatsapp || '+51',
          moneda: this.tienda.moneda as 'SOLES' | 'DOLARES',
          descripcion: this.tienda.descripcion || '',
          direccion: this.tienda.direccion || '',
          horarios: this.tienda.horarios || 'Lun - Sáb 9:00 - 21:00',
          mapa_url: this.tienda.mapa_url || '',
          userId: this.tienda.userId,
          activo: this.tienda.activo
        });

        this.form.get('slug')?.disable({ emitEvent: false });

        // ← Selección segura del plan (se ejecuta después de cargar los planes)
        this.seleccionarPlanActual();
      } else {
        // === MODO CREACIÓN ===
        this.isEditMode = false;
        this.logoPreview = null;
        this.selectedFile = null;

        this.form.reset({
          nombre: '',
          slug: '',
          whatsapp: '+51',
          moneda: 'SOLES',
          descripcion: '',
          direccion: '',
          horarios: 'Lun - Sáb 9:00 - 21:00',
          mapa_url: '',
          userId: null,
          planId: null,
          activo: true
        });

        this.form.get('slug')?.enable({ emitEvent: false });
      }
    }
  }

  private configurarValidadores() {
    const userIdControl = this.form.get('userId');
    const planIdControl = this.form.get('planId');

    if (this.esAdmin) {
      userIdControl?.setValidators([Validators.required, Validators.min(1)]);
      planIdControl?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      userIdControl?.clearValidators();
      planIdControl?.clearValidators();
    }

    userIdControl?.updateValueAndValidity();
    planIdControl?.updateValueAndValidity();
  }

  private cargarUsuarios() {
    this.usuariosLoading = true;
    this.dropTownService.getUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.usuariosLoading = false;
      },
      error: (err) => {
        console.error('Error cargando usuarios:', err);
        this.serverError = 'No se pudieron cargar los usuarios';
        this.usuariosLoading = false;
      }
    });
  }

  private cargarPlanes() {
    this.planesLoading = true;
    this.dropTownService.getPlanes().subscribe({
      next: (data) => {
        this.planes = data;
        this.planesLoading = false;

        // Pre-seleccionar el más barato en creación
        if (!this.isEditMode && this.planes.length > 0 && !this.form.get('planId')?.value) {
          this.form.get('planId')?.setValue(this.planes[0].id);
        }

        // Si estamos editando, intentar seleccionar el plan actual
        if (this.isEditMode) {
          this.seleccionarPlanActual();
        }
      },
      error: (err) => {
        console.error('Error cargando planes:', err);
        this.serverError = 'No se pudieron cargar los planes disponibles';
        this.planesLoading = false;
      }
    });
  }

  /**
   * Busca y selecciona el plan actual de la tienda en el dropdown
   * Usa planSlug o planNombre para coincidir (seguro contra undefined)
   */
  private seleccionarPlanActual(): void {
    if (!this.tienda || this.planes.length === 0) return;

    const planSlug = this.tienda.planSlug?.toLowerCase();
    const planNombre = this.tienda.planNombre?.toLowerCase();

    if (!planSlug && !planNombre) return;

    const planEncontrado = this.planes.find(p => {
      const descLower = p.descripcion.toLowerCase();
      return (planSlug && descLower.includes(planSlug)) ||
             (planNombre && descLower.includes(planNombre));
    });

    if (planEncontrado) {
      this.form.get('planId')?.setValue(planEncontrado.id);
    }
  }

  onNombreChange(): void {
    if (!this.isEditMode) {
      const nombre = this.form.get('nombre')?.value || '';
      if (nombre.length >= 3) {
        const slug = this.tiendaService.generarSlug(nombre);
        this.form.get('slug')?.setValue(slug, { emitEvent: false });
      }
    }
  }

  onFileSelected(event: Event): void {
    this.fileError = null;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      this.fileError = 'Formato inválido. Solo JPG, PNG, GIF o WEBP.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.fileError = 'Imagen demasiado grande. Máximo 5MB.';
      return;
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = () => this.logoPreview = reader.result as string;
    reader.readAsDataURL(file);
  }

  removeLogo(): void {
    this.selectedFile = null;
    this.logoPreview = this.isEditMode ? this.tienda?.logo_img_url || null : null;
    const input = document.getElementById('logoInput') as HTMLInputElement;
    if (input) input.value = '';
    this.fileError = null;
  }

  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control || !control.touched || !control.errors) return '';

    if (control.errors['required']) return 'Este campo es obligatorio';
    if (control.errors['minlength']) return 'Mínimo 3 caracteres';
    if (control.errors['maxlength']) return 'Máximo 50 caracteres';
    if (control.errors['min']) return 'Seleccione una opción válida';
    if (control.errors['pattern']) {
      switch (controlName) {
        case 'slug': return 'Solo letras minúsculas, números y guiones';
        case 'whatsapp': return 'Formato inválido (ej: +51999999999)';
        case 'mapa_url': return 'Debe comenzar con http:// o https://';
        default: return 'Formato inválido';
      }
    }
    return 'Valor inválido';
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid || this.fileError) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = null;

    try {
      let resultado: TiendaResponse;

      const commonData = {
        nombre: this.form.value.nombre!.trim(),
        slug: this.isEditMode ? this.tienda!.slug : this.form.value.slug!.trim(),
        whatsapp: this.form.value.whatsapp?.trim() || undefined,
        moneda: this.form.value.moneda!,
        descripcion: this.form.value.descripcion?.trim() || undefined,
        direccion: this.form.value.direccion?.trim() || undefined,
        horarios: this.form.value.horarios?.trim() || undefined,
        mapa_url: this.form.value.mapa_url?.trim() || undefined,
      };

      if (this.isEditMode && this.tienda?.id) {
        const updateRequest: TiendaUpdateRequest = {
          ...commonData,
          planId: this.esAdmin ? this.form.value.planId ?? undefined : undefined,
          activo: this.esAdmin ? this.form.value.activo ?? undefined : undefined
        };

        resultado = await lastValueFrom(
          this.tiendaService.actualizarTienda(this.tienda.id, updateRequest, this.selectedFile || undefined)
        );
      } else {
        const createRequest: TiendaCreateRequest = {
          ...commonData,
          userId: this.esAdmin ? this.form.value.userId ?? undefined : undefined,
          planId: this.esAdmin ? this.form.value.planId ?? undefined : undefined,
          activo: this.esAdmin ? this.form.value.activo ?? true : undefined
        };

        resultado = await lastValueFrom(
          this.tiendaService.crearTienda(createRequest, this.selectedFile || undefined)
        );
      }

      this.success.emit(resultado);
    } catch (err: any) {
      console.error('Error al guardar tienda:', err);
      this.serverError = err.message || 'Error al guardar la tienda';

      if (err.message?.toLowerCase().includes('slug')) {
        this.serverError = 'El slug ya está en uso por otra tienda';
        this.form.get('slug')?.setErrors({ duplicate: true });
      }
    } finally {
      this.loading = false;
    }
  }

  get planActualTexto(): string {
    if (!this.esAdmin) {
      return this.isEditMode 
        ? `Plan actual: ${this.tienda?.planNombre || 'Desconocido'}`
        : 'Se asignará el plan básico al crear la tienda';
    }
    return '';
  }

  onCancel(): void {
    this.cancel.emit();
  }
}