import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { lastValueFrom } from 'rxjs';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';
import { AuthService } from '../../../../../auth/auth.service';
import { TiendaResponse } from '../../../../model/admin/tienda-admin.model';
import { TiendaAdminService, TiendaCreateRequest, TiendaUpdateRequest } from '../../../../service/service-admin/tienda-admin.service';
import { SwalService } from '../../../../service/SweetAlert/swal.service';
import { GlobalImageFallbackDirective } from '../../../../directives/global-image-fallback.directive';

@Component({
  selector: 'app-form-stores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, GlobalImageFallbackDirective],
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
  formSubmitted = false;

  usuarios: DropTownStandar[] = [];
  usuariosLoading = false;
  planes: DropTownStandar[] = [];
  planesLoading = false;

  esAdmin = false;
  showAppPassword = false; // Por defecto oculto

  form = new FormGroup({
    nombre: new FormControl<string>('', {
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(100)],
      nonNullable: true
    }),
    slug: new FormControl<string>('', {
      validators: [
        Validators.required,
        Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
        Validators.minLength(3),
        Validators.maxLength(60)
      ],
      nonNullable: true
    }),
    whatsapp: new FormControl<string>('+51', {
      validators: [Validators.pattern(/^\+?[0-9\s-]{9,18}$/)]
    }),
    moneda: new FormControl<'SOLES' | 'DOLARES'>('SOLES', {
      validators: [Validators.required],
      nonNullable: true
    }),
    descripcion: new FormControl<string | null>(null),
    direccion: new FormControl<string | null>(null),
    horarios: new FormControl<string | null>(null),
    mapa_url: new FormControl<string | null>(null, {
      validators: [Validators.pattern(/^https?:\/\/[^\s/$.?#].[^\s]*$/i)]
    }),
    emailRemitente: new FormControl<string | null>(null, {
      validators: [Validators.email, Validators.maxLength(150)]
    }),
    emailAppPassword: new FormControl<string | null>(null),
    userId: new FormControl<number | null>(null),
    planId: new FormControl<number | null>(null),
    activo: new FormControl<boolean>(true, { nonNullable: true })
  });

  constructor(
    private tiendaService: TiendaAdminService,
    private dropTownService: DropTownService,
    private auth: AuthService,
    private sweet: SwalService
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.auth.isAdmin();
    this.configurarValidadoresDinamicos();

    if (this.esAdmin) {
      this.cargarUsuarios();
      this.cargarPlanes();
    }

    this.form.get('nombre')?.valueChanges.subscribe(nombre => {
      if (!this.isEditMode && nombre?.trim()) {
        const slug = this.tiendaService.generarSlug(nombre);
        this.form.get('slug')?.setValue(slug, { emitEvent: false });
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tienda'] && changes['tienda'].currentValue !== changes['tienda'].previousValue) {
      this.isEditMode = !!this.tienda;

      if (this.tienda) {
        // En modo edición → MOSTRAMOS SIEMPRE los valores reales (incluyendo sensibles)
        this.logoPreview = this.tienda.logo_img_url || null;
        this.form.patchValue({
          nombre: this.tienda.nombre,
          slug: this.tienda.slug,
          whatsapp: this.tienda.whatsapp || '+51',
          moneda: this.tienda.moneda as 'SOLES' | 'DOLARES',
          descripcion: this.tienda.descripcion,
          direccion: this.tienda.direccion,
          horarios: this.tienda.horarios,
          mapa_url: this.tienda.mapa_url,
          emailRemitente: this.tienda.emailRemitente || null,    // ← Mostramos el correo real
          emailAppPassword: this.tienda.emailAppPassword || null, // ← Mostramos la contraseña real (peligroso pero como pediste)
          userId: this.tienda.userId,
          planId: this.tienda.planId,
          activo: this.tienda.activo ?? true
        });

        this.form.get('slug')?.disable({ emitEvent: false });
      } else {
        // Creación → valores vacíos
        this.logoPreview = null;
        this.selectedFile = null;
        this.form.reset({
          nombre: '',
          slug: '',
          whatsapp: '+51',
          moneda: 'SOLES',
          descripcion: null,
          direccion: null,
          horarios: null,
          mapa_url: null,
          emailRemitente: null,
          emailAppPassword: null,
          userId: null,
          planId: null,
          activo: true
        });

        this.form.get('slug')?.enable({ emitEvent: false });
      }

      this.serverError = null;
      this.fileError = null;
      this.formSubmitted = false;
    }
  }

  private configurarValidadoresDinamicos(): void {
    const userId = this.form.get('userId');
    const planId = this.form.get('planId');

    if (this.esAdmin) {
      userId?.setValidators([Validators.required, Validators.min(1)]);
      planId?.setValidators([Validators.required, Validators.min(1)]);
    } else {
      userId?.clearValidators();
      planId?.clearValidators();
    }

    userId?.updateValueAndValidity();
    planId?.updateValueAndValidity();
  }

  private async cargarPlanes(): Promise<void> {
    this.planesLoading = true;
    try {
      const data = await lastValueFrom(this.dropTownService.getPlanes());
      this.planes = data || [];

      if (!this.isEditMode && this.planes.length > 0) {
        this.form.get('planId')?.setValue(this.planes[0].id, { emitEvent: true });
      }

      if (this.isEditMode && this.tienda?.planId) {
        const plan = this.planes.find(p => p.id === this.tienda?.planId);
        if (plan) this.form.get('planId')?.setValue(plan.id);
      }
    } catch (e) {
      this.sweet.error('Error', 'No se pudieron cargar los planes');
    } finally {
      this.planesLoading = false;
    }
  }

  private cargarUsuarios(): void {
    this.usuariosLoading = true;
    this.dropTownService.getUsuarios().subscribe({
      next: (data) => {
        this.usuarios = data || [];
        this.usuariosLoading = false;
      },
      error: () => {
        this.sweet.error('Error', 'No se pudieron cargar los usuarios');
        this.usuariosLoading = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    this.fileError = null;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) return;

    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      this.fileError = 'Solo se permiten JPG, PNG o WEBP';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.fileError = 'La imagen no debe superar los 5MB';
      return;
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = () => (this.logoPreview = reader.result as string);
    reader.readAsDataURL(file);
  }

  removeLogo(): void {
    this.selectedFile = null;
    this.logoPreview = this.isEditMode ? this.tienda?.logo_img_url ?? null : null;
    const input = document.getElementById('logoInput') as HTMLInputElement;
    if (input) input.value = '';
    this.fileError = null;
  }

  toggleAppPasswordVisibility(): void {
    this.showAppPassword = !this.showAppPassword;
  }

  getErrorMessage(controlName: keyof typeof this.form.controls): string {
    const control = this.form.get(controlName);
    if (!control?.errors) return '';

    if (control.errors['required']) return 'Campo obligatorio';
    if (control.errors['minlength'])
      return `Mínimo ${control.errors['minlength'].requiredLength} caracteres`;
    if (control.errors['maxlength'])
      return `Máximo ${control.errors['maxlength'].requiredLength} caracteres`;
    if (control.errors['email']) return 'Formato de email inválido';
    if (control.errors['pattern'] || control.errors['minlength'] || control.errors['maxlength']) {
      if (controlName === 'emailAppPassword') return 'Debe tener exactamente 16 caracteres alfanuméricos';
      if (controlName === 'slug') return 'Solo minúsculas, números y guiones (sin guion al inicio/final)';
      if (controlName === 'whatsapp') return 'Formato inválido (ej: +51987654321)';
      if (controlName === 'mapa_url') return 'Debe ser una URL válida (http/https)';
    }

    return 'Valor inválido';
  }

  async onSubmit(): Promise<void> {
    this.formSubmitted = true;
    this.form.markAllAsTouched();

    const appPass = this.form.value.emailAppPassword?.trim();
    if (appPass) {
      if (appPass.length !== 16 || !/^[A-Za-z0-9]{16}$/.test(appPass)) {
        this.form.get('emailAppPassword')?.setErrors({ invalidAppPass: true });
        return;
      }
    }

    if (this.form.invalid || this.fileError) {
      this.sweet.warning('Formulario incompleto', 'Revisa los campos marcados en rojo');
      return;
    }

    if (appPass) {
      const confirmed = await this.sweet.confirmAction({
        title: '¡Acción crítica!',
        text: 'Estás modificando la contraseña de aplicación Gmail.\nEsto afectará el envío de correos.',
        confirmButtonText: 'Sí, cambiar',
        icon: 'warning'
      });

      if (!confirmed.isConfirmed) {
        this.form.get('emailAppPassword')?.setValue(null);
        return;
      }
    }

    this.loading = true;
    this.serverError = null;

    try {
      const common = {
        nombre: this.form.value.nombre!.trim(),
        slug: this.isEditMode ? this.tienda!.slug : this.form.value.slug!.trim(),
        whatsapp: this.form.value.whatsapp?.trim() || undefined,
        moneda: this.form.value.moneda!,
        descripcion: this.form.value.descripcion?.trim() || undefined,
        direccion: this.form.value.direccion?.trim() || undefined,
        horarios: this.form.value.horarios?.trim() || undefined,
        mapa_url: this.form.value.mapa_url?.trim() || undefined,
        emailRemitente: this.form.value.emailRemitente?.trim() || undefined,
        emailAppPassword: appPass || undefined
      };

      let response: TiendaResponse;

      if (this.isEditMode && this.tienda?.id) {
        const request: TiendaUpdateRequest = {
          ...common,
          planId: this.form.value.planId ?? undefined,
          activo: this.form.value.activo
        };
        response = await lastValueFrom(
          this.tiendaService.actualizarTienda(this.tienda.id, request, this.selectedFile ?? undefined)
        );
      } else {
        const request: TiendaCreateRequest = {
          ...common,
          userId: this.form.value.userId ?? undefined,
          planId: this.form.value.planId ?? undefined,
          activo: true
        };
        response = await lastValueFrom(
          this.tiendaService.crearTienda(request, this.selectedFile ?? undefined)
        );
      }

      this.sweet.success(
        this.isEditMode ? 'Tienda actualizada' : 'Tienda creada exitosamente!',
        'Operación completada'
      );

      // Solo limpiamos la contraseña después de guardar exitosamente
      this.form.get('emailAppPassword')?.setValue(null);
      this.success.emit(response);
    } catch (error: any) {
      console.error('Error guardando tienda:', error);
      let msg = 'No se pudo guardar la tienda';
      if (error?.error?.message?.includes('slug')) {
        msg = 'El slug ya está en uso';
        this.form.get('slug')?.setErrors({ duplicate: true });
      }
      this.serverError = msg;
      this.sweet.error('Error', msg);
    } finally {
      this.loading = false;
    }
  }

  onCancel(): void {
    this.formSubmitted = false;
    this.cancel.emit();
  }

  debugForm(): void {
    console.clear();
    console.group('🔍 DEBUG FORMULARIO');
    console.log('Inválido?', this.form.invalid);
    console.log('Submitted?', this.formSubmitted);
    console.log('Valores:', this.form.value);
    
    console.log('\nErrores:');
    Object.keys(this.form.controls).forEach(key => {
      const c = this.form.get(key);
      if (c?.invalid) {
        console.warn(`→ ${key}:`, c.errors);
      }
    });
    console.groupEnd();
  }
}