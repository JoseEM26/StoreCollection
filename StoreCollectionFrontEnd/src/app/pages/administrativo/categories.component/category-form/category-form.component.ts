// src/app/pages/administrativo/categories/category-form/category-form.component.ts

import { Component, EventEmitter, Input, Output, signal, effect, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaAdminService } from '../../../../service/service-admin/categoria-admin.service';
import { AuthService } from '../../../../../auth/auth.service';
import { CategoriaResponse, CategoriaRequest } from '../../../../model/admin/categoria-admin.model';
import { DropTownService, DropTownStandar } from '../../../../service/droptown.service';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './category-form.component.html',
  styleUrl: './category-form.component.css'
})
export class CategoryFormComponent implements OnInit {
  @Input() isEdit = false;
  @Input() categoria?: CategoriaResponse;

  @Output() saved = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  // Signals
  nombre = signal<string>('');
  tiendaId = signal<number | null>(null);
  tiendas = signal<DropTownStandar[]>([]);

  loading = signal(false);
  errorMessage = signal<string>('');

  constructor(
    private categoriaService: CategoriaAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService
  ) {}

  ngOnInit(): void {
    // Solo cargar tiendas si es ADMIN
    if (this.auth.isAdmin()) {
      this.dropTownService.getTiendas().subscribe({
        next: (data) => this.tiendas.set(data),
        error: () => this.errorMessage.set('Error al cargar las tiendas')
      });
    }
  }

  ngOnChanges(): void {
    if (this.isEdit && this.categoria) {
      this.nombre.set(this.categoria.nombre);
      // Si es ADMIN, preseleccionar la tienda actual
      if (this.auth.isAdmin()) {
        this.tiendaId.set(this.categoria.tiendaId);
      }
    } else {
      this.nombre.set('');
      this.tiendaId.set(null);
    }
    this.errorMessage.set('');
  }

  save(): void {
    const nombreTrim = this.nombre().trim();
    if (!nombreTrim) {
      this.errorMessage.set('El nombre de la categoría es obligatorio');
      return;
    }

    if (this.auth.isAdmin() && !this.tiendaId()) {
      this.errorMessage.set('Debes seleccionar una tienda');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const request: CategoriaRequest = {
      nombre: nombreTrim,
      slug: this.generarSlug(nombreTrim)  // Puedes mejorarlo después
    };

    // Solo ADMIN envía tiendaId
    if (this.auth.isAdmin() && this.tiendaId()) {
      request.tiendaId = this.tiendaId()!;
    }

    const obs = this.isEdit && this.categoria
      ? this.categoriaService.actualizarCategoria(this.categoria.id, request)
      : this.categoriaService.crearCategoria(request);

    obs.subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Error al guardar la categoría';
        this.errorMessage.set(msg);
      }
    });
  }

  // Utilidad simple para generar slug (opcional, puedes mejorarla)
  private generarSlug(nombre: string): string {
    return nombre
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  cancel(): void {
    this.closed.emit();
  }
}