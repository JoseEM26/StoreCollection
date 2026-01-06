// src/app/pages/admin/stores/form-stores/form-stores.component.ts
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
  imports: [CommonModule, ReactiveFormsModule,GlobalImageFallbackDirective],
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

  // Control para mostrar/ocultar contraseña de app
  showAppPassword = false;

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
    
    // Nuevos campos
    emailRemitente: new FormControl<string>('', [
      Validators.email,
      Validators.maxLength(150)
    ]),
    emailAppPassword: new FormControl<string>('', [
      Validators.minLength(16),
      Validators.maxLength(16),
      Validators.pattern(/^[A-Za-z0-9]+$/)
    ]),

    userId: new FormControl<number | null>(null),
    planId: new FormControl<number | null>(null),
    activo: new FormControl<boolean>(true)
  });

  constructor(
    private tiendaService: TiendaAdminService,
    private dropTownService: DropTownService,
    private auth: AuthService,
    private sweet: SwalService
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
    if (changes['tienda']?.currentValue !== changes['tienda']?.previousValue) {
      if (this.tienda) {
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
          emailRemitente: this.tienda.emailRemitente || '',
          // ¡CAMBIO! Ahora SÍ mostramos la contraseña actual (como pediste)
          emailAppPassword: this.tienda.emailAppPassword || '',
          userId: this.tienda.userId,
          activo: this.tienda.activo
        });

        this.form.get('slug')?.disable({ emitEvent: false });
        this.seleccionarPlanActual();
      } else {
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
          emailRemitente: '',
          emailAppPassword: '',
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
      error: () => {
        this.sweet.error('Error', 'No se pudieron cargar los usuarios');
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

        if (!this.isEditMode && this.planes.length > 0) {
          this.form.get('planId')?.setValue(this.planes[0].id);
        }

        if (this.isEditMode) {
          this.seleccionarPlanActual();
        }
      },
      error: () => {
        this.sweet.error('Error', 'No se pudieron cargar los planes');
        this.planesLoading = false;
      }
    });
  }

  private seleccionarPlanActual(): void {
    if (!this.tienda || this.planes.length === 0) return;

    const planSlug = this.tienda.planSlug?.toLowerCase();
    const planNombre = this.tienda.planNombre?.toLowerCase();

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
      this.fileError = 'Solo se permiten JPG, PNG, GIF o WEBP';
      this.sweet.warning('Formato inválido', this.fileError);
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.fileError = 'La imagen no debe superar los 5MB';
      this.sweet.warning('Imagen demasiado grande', this.fileError);
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

  toggleAppPasswordVisibility(): void {
    this.showAppPassword = !this.showAppPassword;
  }

  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (!control || !control.touched || !control.errors) return '';

    if (control.errors['required']) return 'Campo obligatorio';
    if (control.errors['minlength']) return 'Mínimo 3 caracteres';
    if (control.errors['maxlength']) return 'Máximo 50 caracteres';
    if (control.errors['email']) return 'Correo electrónico inválido';
    if (control.errors['pattern']) {
      if (controlName === 'emailAppPassword') {
        return 'Debe tener exactamente 16 caracteres alfanuméricos';
      }
      if (controlName === 'slug') return 'Solo minúsculas, números y guiones';
      if (controlName === 'whatsapp') return 'Formato inválido (+51 seguido de 9 dígitos)';
      if (controlName === 'mapa_url') return 'Debe ser una URL válida';
    }
    return 'Valor inválido';
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid || this.fileError) {
      this.form.markAllAsTouched();
      this.sweet.warning('Formulario incompleto', 'Revisa los campos marcados');
      return;
    }

    // Advertencia especial si se está intentando cambiar la contraseña de app
    if (this.form.value.emailAppPassword && this.form.value.emailAppPassword.trim().length > 0) {
      const result = await this.sweet.confirmAction({
        title: '¿Estás seguro?',
        text: 'Estás modificando la contraseña de aplicación de Gmail.\nEsta acción es sensible y afectará el envío de correos.',
        confirmButtonText: 'Sí, cambiar contraseña',
        icon: 'warning'
      });

      if (!result.isConfirmed) {
        this.form.get('emailAppPassword')?.setValue('');
        return;
      }
    }

    this.loading = true;
    this.serverError = null;

    const loadingSwal = this.sweet.loading(
      this.isEditMode ? 'Actualizando tienda...' : 'Creando tienda...'
    );

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
        emailRemitente: this.form.value.emailRemitente?.trim() || undefined,
        emailAppPassword: this.form.value.emailAppPassword?.trim() || undefined
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

      this.sweet.success(
        this.isEditMode ? '¡Tienda actualizada!' : '¡Tienda creada!',
        'Los cambios han sido guardados correctamente'
      );

      // Limpiamos el campo sensible después de guardar
      this.form.get('emailAppPassword')?.setValue('');

      this.success.emit(resultado);
    } catch (err: any) {
      console.error('Error al guardar tienda:', err);

      let mensaje = 'No se pudo guardar la tienda';
      if (err.message?.toLowerCase().includes('slug')) {
        mensaje = 'El slug ya está en uso por otra tienda';
        this.form.get('slug')?.setErrors({ duplicate: true });
      } else if (err.message?.includes('16 caracteres')) {
        mensaje = 'La contraseña de aplicación debe tener exactamente 16 caracteres';
      }

      this.sweet.error('Error al guardar', mensaje);
      this.serverError = mensaje;
    } finally {
      this.loading = false;
      this.sweet.close(); // Cierra el loading
    }
  }
get emailAppPasswordControl() {
  return this.form.get('emailAppPassword')!; // Esto elimina la advertencia de TS
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