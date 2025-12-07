// src/app/components/form-stores/form-stores.component.ts
import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { TiendaResponse } from '../../../../model/admin/tienda-admin.model';
import { TiendaAdminService, TiendaCreateRequest } from '../../../../service/service-admin/tienda-admin.service';
import { lastValueFrom } from 'rxjs';

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

  form!: FormGroup;

  planes = [
    { id: 1, nombre: 'Básico' },
    { id: 2, nombre: 'Pro' },
    { id: 3, nombre: 'Enterprise' }
  ];

  constructor(
    private fb: FormBuilder,
    private tiendaService: TiendaAdminService
  ) {
    this.createForm();
  }

  ngOnInit(): void {
    // El formulario ya se crea en el constructor
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tienda']) {
      if (this.tienda) {
        this.isEditMode = true;
        this.cargarDatosTienda();
      } else {
        this.isEditMode = false;
        this.resetForm();
      }
    }
  }

  private createForm() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      slug: ['', [
        Validators.required,
        Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
        Validators.minLength(3),
        Validators.maxLength(40)
      ]],
      whatsapp: ['+51', [Validators.pattern(/^\+\d{1,3}\s?\d{6,14}$/)]],
      moneda: ['SOLES'],
      planId: [1, Validators.required],
      logo_img_url: [''],
      descripcion: ['', Validators.maxLength(500)],
      direccion: ['', Validators.maxLength(200)],
      horarios: ['Lun - Sáb 9:00 - 21:00', Validators.maxLength(100)],
      mapa_url: ['', this.urlValidator]
    });

    // Escuchar cambios en nombre para generar slug automáticamente (solo en creación)
    this.form.get('nombre')?.valueChanges.subscribe(nombre => {
      if (!this.isEditMode && nombre) {
        const slug = this.tiendaService.generarSlug(nombre);
        this.form.get('slug')?.setValue(slug, { emitEvent: false });
      }
    });

    // Escuchar cambios en logo URL
    this.form.get('logo_img_url')?.valueChanges.subscribe(url => {
      this.logoPreview = url || null;
    });
  }

  private cargarDatosTienda() {
    this.form.patchValue({
      nombre: this.tienda!.nombre,
      slug: this.tienda!.slug,
      whatsapp: this.tienda!.whatsapp || '+51',
      moneda: this.tienda!.moneda,
      descripcion: this.tienda!.descripcion || '',
      direccion: this.tienda!.direccion || '',
      horarios: this.tienda!.horarios || 'Lun - Sáb 9:00 - 21:00',
      mapa_url: this.tienda!.mapa_url || '',
      logo_img_url: this.tienda!.logo_img_url || '',
      planId: this.tienda!.planId || 1
    });

    this.logoPreview = this.tienda!.logo_img_url || null;
  }

  resetForm() {
    this.form.reset({
      nombre: '',
      slug: '',
      whatsapp: '+51',
      moneda: 'SOLES',
      descripcion: '',
      direccion: '',
      horarios: 'Lun - Sáb 9:00 - 21:00',
      mapa_url: '',
      logo_img_url: '',
      planId: 1
    });
    this.logoPreview = null;
  }

  // Validador personalizado para URLs
  urlValidator(control: AbstractControl) {
    if (!control.value) return null;
    const urlPattern = /^https?:\/\/[\w\-]+(\.[\w\-]+)+[/#?]?.*$/;
    return urlPattern.test(control.value) ? null : { invalidUrl: true };
  }

getError(field: string): string {
  const control = this.form.get(field);

  if (!control || !control.touched || control.pending) return '';
  if (control.disabled) return '';

  // Nombre
  if (field === 'nombre') {
    if (control.hasError('required')) return 'Por favor, escribe el nombre de tu tienda';
    if (control.hasError('minlength')) return 'El nombre debe tener al menos 3 caracteres';
    if (control.hasError('maxlength')) return 'El nombre no puede tener más de 50 caracteres';
  }

  // Slug
  if (field === 'slug') {
    if (control.hasError('required')) return 'La URL personalizada es obligatoria';
    if (control.hasError('minlength')) return 'La URL debe tener al menos 3 caracteres';
    if (control.hasError('maxlength')) return 'La URL no puede tener más de 40 caracteres';
    if (control.hasError('pattern')) return 'Solo letras minúsculas, números y guiones (-). Ejemplo: mi-tienda-2025';
  }

  // WhatsApp
  if (field === 'whatsapp') {
    if (control.hasError('pattern')) return 'Formato inválido. Ejemplo válido: +51 987654321 o +51987654321';
  }

  // Logo URL
  if (field === 'logo_img_url') {
    if (control.hasError('invalidUrl')) return 'La URL del logo debe comenzar con https:// y ser válida';
  }

  // Mapa URL
  if (field === 'mapa_url') {
    if (control.hasError('invalidUrl')) return 'Ingresa una URL válida de Google Maps (debe empezar con https://)';
  }

  // Descripción
  if (field === 'descripcion') {
    if (control.hasError('maxlength')) return 'La descripción no puede exceder los 500 caracteres';
  }

  // Dirección
  if (field === 'direccion') {
    if (control.hasError('maxlength')) return 'La dirección no puede tener más de 200 caracteres';
  }

  // Horarios
  if (field === 'horarios') {
    if (control.hasError('maxlength')) return 'Los horarios no pueden exceder los 100 caracteres';
  }

  return 'Este campo tiene un error';
}

  async onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    try {
      const formValue = this.form.value as TiendaCreateRequest;
      let resultado: TiendaResponse;

      if (this.isEditMode && this.tienda?.id) {
        resultado = await lastValueFrom(
          await this.tiendaService.actualizarTienda(this.tienda.id, formValue)
        );
      } else {
        resultado = await lastValueFrom(
          this.tiendaService.crearTienda(formValue)
        );
      }

      this.success.emit(resultado);
    } catch (err: any) {
      console.error('Error:', err);
      alert(err.message || 'Error al guardar la tienda');
    } finally {
      this.loading = false;
    }
  }

  onLogoError(event: any) {
    event.target.src = 'https://via.placeholder.com/300/6366f1/ffffff?text=LOGO';
  }

  onCancel() {
    this.cancel.emit();
  }
}