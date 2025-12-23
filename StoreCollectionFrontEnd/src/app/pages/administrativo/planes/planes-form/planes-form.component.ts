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
    precio: 0,
    maxProductos: 100,
    mesInicio: 1,
    mesFin: 12
  });

  isEdit = signal(false);
  loading = signal(false);

  meses = [
    { value: 1, label: 'Enero' }, { value: 2, label: 'Febrero' }, { value: 3, label: 'Marzo' },
    { value: 4, label: 'Abril' }, { value: 5, label: 'Mayo' }, { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' }, { value: 8, label: 'Agosto' }, { value: 9, label: 'Septiembre' },
    { value: 10, label: 'Octubre' }, { value: 11, label: 'Noviembre' }, { value: 12, label: 'Diciembre' }
  ];

  constructor(private planService: PlanAdminService) {}

  ngOnChanges(): void {
    if (this.plan) {
      this.isEdit.set(true);
      this.form.set({
        nombre: this.plan.nombre || '',
        precio: this.plan.precio || 0,
        maxProductos: this.plan.maxProductos || 100,
        mesInicio: this.plan.mesInicio || 1,
        mesFin: this.plan.mesFin || 12
      });
    } else {
      this.isEdit.set(false);
      this.form.set({
        nombre: '',
        precio: 0,
        maxProductos: 100,
        mesInicio: 1,
        mesFin: 12
      });
    }
  }

  onSubmit(): void {
    const data = this.form();
    if (!data.nombre.trim() || data.precio < 0) {
      alert('Por favor completa los campos obligatorios correctamente');
      return;
    }

    this.loading.set(true);

    const observable = this.isEdit() && this.plan
      ? this.planService.actualizar(this.plan.id, data)
      : this.planService.crear(data);

    observable.subscribe({
      next: () => {
        this.loading.set(false);
        this.success.emit();
      },
      error: (err) => {
        console.error(err);
        this.loading.set(false);
        alert('Error al guardar el plan');
      }
    });
  }

  onCancel(): void {
    this.cancel.emit();
  }
}