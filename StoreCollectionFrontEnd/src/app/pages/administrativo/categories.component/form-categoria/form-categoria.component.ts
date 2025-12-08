// src/app/pages/administrativo/categories/form-categoria/form-categoria.component.ts
import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaAdminService, CategoriaRequest, CategoriaResponse } from '../../../../service/service-admin/categoria-admin.service';
import { AuthService } from '../../../../../auth/auth.service';
import { TiendaAdminService } from '../../../../service/service-admin/tienda-admin.service'; // ← CORRECTO
import { TiendaDropdown } from '../../../../model/index.dto';
import { lastValueFrom } from 'rxjs';
import Swal from 'sweetalert2';
import { Router } from '@angular/router';

@Component({
  selector: 'app-form-categoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './form-categoria.component.html',
  styleUrl: './form-categoria.component.css'
})
export class FormCategoriaComponent implements OnInit, OnChanges, OnDestroy {
  @Input() categoria?: CategoriaResponse | null = null;
  @Output() success = new EventEmitter<CategoriaResponse>();
  @Output() cancel = new EventEmitter<void>();

  form: CategoriaRequest & { tiendaId?: number } = {
    nombre: '',
    slug: '',
    tiendaId: undefined
  };

  loading = false;
  esAdmin = false;
  tiendas: TiendaDropdown[] = [];
  dropdownCargado = false;

  constructor(
    private categoriaService: CategoriaAdminService,
    private authService: AuthService,
    private tiendaAdminService: TiendaAdminService, // ← Usa el servicio correcto
    private router: Router
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.authService.isAdmin();

    // Suscripción al dropdown de tiendas (reactivo y automático)
    this.tiendaAdminService.tiendasDropdown$.subscribe({
      next: (tiendas) => {
        this.tiendas = tiendas;
        this.dropdownCargado = true;

        // Si es OWNER y tiene solo 1 tienda → preseleccionar automáticamente
        if (!this.esAdmin && tiendas.length === 1) {
          this.form.tiendaId = tiendas[0].id;
        }

        // Si estamos editando y ya hay tiendaId, asegurarnos que esté seleccionado
        if (this.categoria?.tiendaId && this.esAdmin) {
          this.form.tiendaId = this.categoria.tiendaId;
        }
      },
      error: () => {
        this.tiendas = [];
        this.dropdownCargado = true;
      }
    });

    // Forzar carga inicial por si no se perdió
    this.tiendaAdminService.cargarDropdownTiendas();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['categoria'] && this.categoria) {
      this.form = {
        nombre: this.categoria.nombre,
        slug: this.categoria.slug,
        tiendaId: this.categoria.tiendaId
      };
    } else if (changes['categoria'] && !this.categoria) {
      this.resetForm();
    }
  }

  ngOnDestroy(): void {
    // No necesario con takeUntil si usas async pipe, pero por seguridad
  }

  resetForm() {
    this.form = { nombre: '', slug: '', tiendaId: undefined };
  }

  onNombreChange() {
    if (!this.categoria?.id) {
      this.form.slug = this.categoriaService.generarSlug(this.form.nombre);
    }
  }

  async onSubmit() {
    // Validaciones
    if (!this.form.nombre.trim()) {
      Swal.fire('Nombre requerido', 'Debes ingresar un nombre para la categoría', 'warning');
      return;
    }

    if (!this.form.slug.trim()) {
      Swal.fire('Slug requerido', 'El slug es obligatorio', 'warning');
      return;
    }

    if (this.esAdmin && !this.form.tiendaId) {
      Swal.fire('Tienda obligatoria', 'Como administrador debes seleccionar una tienda', 'warning');
      return;
    }

    if (!this.esAdmin && this.tiendas.length === 0) {
      Swal.fire({
        icon: 'info',
        title: 'Primero crea tu tienda',
        text: 'No puedes crear categorías sin tener una tienda activa.',
        confirmButtonText: 'Ir a crear tienda',
        allowOutsideClick: false,
        preConfirm: () => {
          this.router.navigate(['/admin/tienda']);
        }
      });
      return;
    }

    this.loading = true;

    const payload: any = {
      nombre: this.form.nombre.trim(),
      slug: this.form.slug.trim()
    };

    // Solo ADMIN envía tiendaId
    if (this.esAdmin && this.form.tiendaId) {
      payload.tiendaId = this.form.tiendaId;
    }

    try {
      const resultado = this.categoria?.id
        ? await lastValueFrom(this.categoriaService.actualizarCategoria(this.categoria.id, payload))
        : await lastValueFrom(this.categoriaService.crearCategoria(payload));

      Swal.fire({
        icon: 'success',
        title: '¡Listo!',
        text: this.categoria?.id ? 'Categoría actualizada' : 'Categoría creada correctamente',
        timer: 1800,
        showConfirmButton: false
      });

      this.success.emit(resultado);
      this.resetForm();
    } catch (error: any) {
      const msg = error.error?.message || error.message || 'Error desconocido';

      if (msg.toLowerCase().includes('tienda') && !this.esAdmin) {
        Swal.fire({
          icon: 'warning',
          title: 'Falta tu tienda',
          html: `
            <p>${msg}</p>
            <p class="mt-3"><strong>Debes crear una tienda antes de continuar.</strong></p>
          `,
          confirmButtonText: 'Crear mi tienda ahora',
          cancelButtonText: 'Cancelar',
          showCancelButton: true,
          allowOutsideClick: false
        }).then((result) => {
          if (result.isConfirmed) {
            this.router.navigate(['/admin/tienda']);
          }
        });
      } else {
        Swal.fire('Error', msg, 'error');
      }
    } finally {
      this.loading = false;
    }
  }

  onCancel() {
    this.cancel.emit();
  }
}