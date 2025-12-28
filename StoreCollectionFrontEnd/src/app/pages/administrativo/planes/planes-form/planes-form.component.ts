// src/app/pages/administrativo/planes/planes-form/planes-form.component.ts
import { Component, EventEmitter, Input, OnChanges, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlanAdminService } from '../../../../service/service-admin/plan-admin.service';
import { PlanRequest, PlanResponse } from '../../../../model/admin/plan-admin.model';

@Component({
  selector: 'app-planes-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './planes-form.component.html',
  styleUrl: './planes-form.component.css'
})
export class PlanesFormComponent implements OnChanges {
  @Input() plan: PlanResponse | null = null;
  @Output() success = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

form = signal<PlanRequest>({
  nombre: '',
  slug: '',
  descripcion: '',
  precioMensual: 0,
  precioAnual: undefined,        // ← Cambiado de null a undefined
  intervaloBilling: 'month',
  intervaloCantidad: 1,
  duracionDias: undefined,       // ← Cambiado de null a undefined
  maxProductos: 100,
  maxVariantes: 500,
  esTrial: false,
  diasTrial: 0,
  esVisiblePublico: true,
  orden: 999,
  activo: true
});

  isEdit = signal(false);
  loading = signal(false);

  intervalos = [
    { value: 'month', label: 'Mensual' },
    { value: 'year', label: 'Anual' }
  ];

  constructor(private planService: PlanAdminService) {}
ngOnChanges(): void {
  if (this.plan) {
    this.isEdit.set(true);
    this.form.set({
      nombre: this.plan.nombre || '',
      slug: this.plan.slug || '',
      descripcion: this.plan.descripcion || '',
      precioMensual: this.plan.precioMensual ?? 0,
      precioAnual: this.plan.precioAnual ?? undefined,     // ← undefined
      intervaloBilling: this.plan.intervaloBilling || 'month',
      intervaloCantidad: this.plan.intervaloCantidad ?? 1,
      duracionDias: this.plan.duracionDias ?? undefined,   // ← undefined
      maxProductos: this.plan.maxProductos ?? 100,
      maxVariantes: this.plan.maxVariantes ?? 500,
      esTrial: !!this.plan.esTrial,
      diasTrial: this.plan.diasTrial ?? 0,
      esVisiblePublico: this.plan.esVisiblePublico !== false,
      orden: this.plan.orden ?? 999,
      activo: this.plan.activo !== false
    });
  } else {
    this.isEdit.set(false);
    this.resetForm();
  }
}

 // En resetForm()
private resetForm(): void {
  this.form.set({
    nombre: '',
    slug: '',
    descripcion: '',
    precioMensual: 0,
    precioAnual: undefined,     // ← undefined
    intervaloBilling: 'month',
    intervaloCantidad: 1,
    duracionDias: undefined,    // ← undefined
    maxProductos: 100,
    maxVariantes: 500,
    esTrial: false,
    diasTrial: 0,
    esVisiblePublico: true,
    orden: 999,
    activo: true
  });
  }

  // Generar slug automático (solo en creación)
  generarSlug(): void {
    if (this.isEdit()) return;  // No regenerar en edición

    const nombre = this.form().nombre.trim();
    if (nombre) {
      const slug = nombre
        .toLowerCase()
        .normalize('NFD')  // Soporte para acentos
        .replace(/[\u0300-\u036f]/g, '')  // Quitar acentos
        .replace(/[^a-z0-9\s-]/g, '')
        .trim()
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');

      this.form.update(f => ({ ...f, slug }));
    }
  }

  onSubmit(): void {
    const data = this.form();

    // Validaciones
    if (!data.nombre.trim()) {
      alert('El nombre del plan es obligatorio');
      return;
    }
    if (!data.slug.trim()) {
      alert('El slug es obligatorio. Escribe el nombre para generarlo automáticamente.');
      return;
    }
    if (data.precioMensual < 0) {
      alert('El precio mensual no puede ser negativo');
      return;
    }
    if (data.esTrial && (!data.diasTrial || data.diasTrial <= 0)) {
      alert('Si activas el trial, debes ingresar un número de días mayor a 0');
      return;
    }
    if (data.maxProductos < 1 || data.maxVariantes < 1) {
      alert('Los límites de productos y variantes deben ser al menos 1');
      return;
    }

    this.loading.set(true);

    const observable = this.isEdit() && this.plan?.id
      ? this.planService.actualizar(this.plan.id, data)
      : this.planService.crear(data);

    observable.subscribe({
      next: () => {
        this.loading.set(false);
        this.success.emit();
      },
      error: (err) => {
        console.error('Error al guardar el plan:', err);
        this.loading.set(false);
        const msg = err.error?.message || err.message || 'Error al guardar el plan';
        alert(msg);
      }
    });
  }

  onCancel(): void {
    this.cancel.emit();
  }

  // Helper para mostrar campos de trial
  get showTrialFields(): boolean {
    return !!this.form().esTrial;
  }

  // Helper para deshabilitar precio anual si no es necesario
  get showPrecioAnual(): boolean {
    return this.form().intervaloBilling === 'year';
  }
}