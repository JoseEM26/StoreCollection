// src/app/pages/administrativo/categories/form-categoria/form-categoria.component.ts
import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaAdminService, CategoriaRequest } from '../../../../service/service-admin/categoria-admin.service';

@Component({
  selector: 'app-form-categoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './form-categoria.component.html',
  styleUrl: './form-categoria.component.css'
})
export class FormCategoriaComponent implements OnChanges {
  @Input() categoria?: any; // si viene → editar, si no → crear
  @Output() success = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  form: CategoriaRequest = {
    nombre: '',
    slug: ''
  };

  loading = false;

  constructor(private categoriaService: CategoriaAdminService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['categoria'] && this.categoria) {
      this.form = {
        nombre: this.categoria.nombre,
        slug: this.categoria.slug
      };
    } else {
      this.form = { nombre: '', slug: '' };
    }
  }

  // Generar slug al escribir el nombre
  onNombreChange() {
    if (!this.categoria) { // solo en creación
      this.form.slug = this.categoriaService.generarSlug(this.form.nombre);
    }
  }

  async onSubmit() {
    if (!this.form.nombre.trim() || !this.form.slug.trim()) {
      alert('Nombre y slug son obligatorios');
      return;
    }

    this.loading = true;

    try {
      let resultado;

      if (this.categoria?.id) {
        resultado = await this.categoriaService
          .crearCategoria(this.form) // tu backend no tiene PUT aún, así que usamos POST también para editar (o lo cambias después)
          .toPromise();
      } else {
        resultado = await this.categoriaService.crearCategoria(this.form).toPromise();
      }

      this.success.emit(resultado);
    } catch (err: any) {
      alert(err.error?.message || 'Error al guardar categoría');
    } finally {
      this.loading = false;
    }
  }

  onCancel() {
    this.cancel.emit();
  }
}