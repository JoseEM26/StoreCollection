import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { TiendaResponse } from '../../../../model/admin/tienda-admin.model';
import { 
  TiendaAdminService, 
  TiendaCreateRequest, 
  TiendaUpdateRequest 
} from '../../../../service/service-admin/tienda-admin.service';
import { lastValueFrom } from 'rxjs';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';
import { AuthService } from '../../../../../auth/auth.service';

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
  planes: DropTownStandar[] = [];        // ← Planes dinámicos del backend
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
    planId: new FormControl<number | null>(null, [Validators.required]),  // ← null inicial
    userId: new FormControl<number>(0),
    activo: new FormControl<boolean>(true)
  });

  constructor(
    private tiendaService: TiendaAdminService,
    private dropTownService: DropTownService,
    private auth: AuthService
  ) {
    this.esAdmin = this.auth.isAdmin();
  }

  ngOnInit(): void {
    this.esAdmin = this.auth.isAdmin();

    // Configurar validadores dinámicos
    this.configurarValidadores();

    // Slug empieza habilitado
    this.form.get('slug')?.enable();

    // Cargar datos del backend SIEMPRE (planes para todos, usuarios solo admin)
    this.cargarPlanes();  // ← Siempre se cargan los 2 planes activos
    
    if (this.esAdmin) {
      this.cargarUsuarios();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tienda']) {
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
          planId: this.tienda.planId ?? null,
          userId: this.tienda.userId,
          activo: this.tienda.activo
        });

        // Si el plan de la tienda no está en los planes activos, lo agregamos
        this.agregarPlanActualSiNoExiste();

        // Slug NO editable en edición
        this.form.get('slug')?.disable({ emitEvent: false });
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
          planId: null,  // ← Espera a que carguen los planes
          userId: 0,
          activo: true
        });

        // Slug SÍ editable en creación
        this.form.get('slug')?.enable({ emitEvent: false });
      }
    }
  }

  private configurarValidadores() {
    if (this.esAdmin) {
      this.form.get('userId')?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      this.form.get('userId')?.clearValidators();
    }
    this.form.get('userId')?.updateValueAndValidity();
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
        this.planes = data;  // ← Solo llegan los 2 planes activos del backend
        this.planesLoading = false;

        // Si estamos editando, verificar si el plan actual existe
        if (this.isEditMode && this.tienda?.planId) {
          this.agregarPlanActualSiNoExiste();
        }

        // Setear plan por defecto si no hay selección
        if (!this.form.get('planId')?.value && this.planes.length > 0) {
          this.form.patchValue({ planId: this.planes[0].id });
        }
      },
      error: (err) => {
        console.error('Error cargando planes:', err);
        this.serverError = 'No se pudieron cargar los planes disponibles';
        this.planesLoading = false;
        // Fallback a planes hardcodeados solo si falla el backend
        this.planes = [
          { id: 1, descripcion: 'Básico' },
          { id: 2, descripcion: 'Pro' }
        ];
      }
    });
  }

  private agregarPlanActualSiNoExiste() {
    if (this.tienda?.planId) {
      const planExiste = this.planes.some(p => p.id === this.tienda!.planId);
      if (!planExiste) {
        // Agregar el plan actual al inicio (solo para mostrar en edición)
        this.planes.unshift({
          id: this.tienda!.planId!,
          descripcion: (this.tienda.planNombre || 'Plan Actual') + ' (actual)'
        });
      }
    }
  }

  // Generar slug automático al escribir nombre (solo en creación)
  onNombreChange() {
    if (!this.isEditMode && this.form.get('nombre')?.valid) {
      const slug = this.tiendaService.generarSlug(this.form.get('nombre')?.value || '');
      this.form.get('slug')?.setValue(slug);
    }
  }

  // Manejo de selección de archivo con validaciones
  onFileSelected(event: any) {
    this.fileError = null;
    const file = event.target.files[0] as File;

    if (file) {
      const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        this.fileError = 'Formato inválido. Solo JPG, PNG, GIF o WEBP.';
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        this.fileError = 'La imagen es demasiado grande. Máximo 5MB.';
        return;
      }

      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.logoPreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  removeLogo() {
    this.selectedFile = null;
    this.logoPreview = this.isEditMode ? this.tienda?.logo_img_url || null : null;
    (document.getElementById('logoInput') as HTMLInputElement).value = '';
    this.fileError = null;
  }

  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control || !control.touched || !control.errors) return '';

    if (control.errors['required']) return 'Este campo es obligatorio';
    if (control.errors['minlength']) return 'Mínimo 3 caracteres';
    if (control.errors['pattern']) {
      switch (controlName) {
        case 'slug': return 'Solo letras minúsculas, números y guiones';
        case 'whatsapp': return 'Formato inválido (ej: +51999999999)';
        case 'mapa_url': return 'Debe ser una URL válida';
        default: return 'Formato inválido';
      }
    }
    if (control.errors['maxlength']) return 'Máximo 50 caracteres';
    if (control.errors['min']) return 'Seleccione un usuario válido';

    return 'Valor inválido';
  }

  async onSubmit() {
    if (this.form.invalid || this.fileError) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.serverError = null;

    try {
      let resultado: TiendaResponse;

      if (this.isEditMode && this.tienda?.id) {
        // EDICIÓN
        const updateRequest: TiendaUpdateRequest = {
          nombre: this.form.value.nombre!.trim(),
          slug: this.form.value.slug!,
          whatsapp: this.form.value.whatsapp?.trim() || undefined,
          moneda: this.form.value.moneda!,
          descripcion: this.form.value.descripcion?.trim() || undefined,
          direccion: this.form.value.direccion?.trim() || undefined,
          horarios: this.form.value.horarios?.trim() || undefined,
          mapa_url: this.form.value.mapa_url?.trim() || undefined,
          planId: this.form.value.planId ?? null,
          activo: this.esAdmin ? (this.form.value.activo ?? undefined) : undefined
        };

        resultado = await lastValueFrom(
          this.tiendaService.actualizarTienda(this.tienda.id, updateRequest, this.selectedFile || undefined)
        );
      } else {
        // CREACIÓN
        const createRequest: TiendaCreateRequest = {
          nombre: this.form.value.nombre!,
          slug: this.form.value.slug!,
          whatsapp: this.form.value.whatsapp ?? undefined,
          moneda: this.form.value.moneda ?? undefined,
          descripcion: this.form.value.descripcion ?? undefined,
          direccion: this.form.value.direccion ?? undefined,
          horarios: this.form.value.horarios ?? undefined,
          mapa_url: this.form.value.mapa_url ?? undefined,
          planId: this.form.value.planId!,  // ← Ahora siempre tiene valor
          userId: this.esAdmin ? this.form.value.userId ?? undefined : undefined,
          activo: this.esAdmin ? this.form.value.activo ?? undefined : undefined
        };

        resultado = await lastValueFrom(
          this.tiendaService.crearTienda(createRequest, this.selectedFile || undefined)
        );
      }

      this.success.emit(resultado);
    } catch (err: any) {
      console.error('Error al guardar tienda:', err);
      this.serverError = err.message || 'Error desconocido al guardar la tienda';

      if (err.message?.toLowerCase().includes('slug')) {
        this.serverError = 'El slug ya está en uso por otra tienda';
        this.form.get('slug')?.setErrors({ serverError: true });
      }
    } finally {
      this.loading = false;
    }
  }

  // ← FUNCIÓN CORREGIDA para usar 'descripcion' en lugar de 'nombre'
  get nombrePlanActual(): string {
    const planId = this.form.value.planId;
    if (!planId || !this.planes?.length) {
      return 'Sin plan asignado';
    }
    const plan = this.planes.find(p => p.id === planId);
    return plan ? plan.descripcion : 'Plan desconocido';
  }

  onCancel() {
    this.cancel.emit();
  }
}