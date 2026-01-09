// src/app/admin/atributo/atributo.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';

import {
  AtributoAdminService,
  AtributoCreateRequest,
  AtributoListItem,
  AtributoUpdateRequest
} from '../../../service/service-admin/atributo-admin.service';
import { AuthService } from '../../../../auth/auth.service';
import { PageResponse } from '../../../service/service-admin/dashboard.service'; // Ajusta la ruta si es necesario
import { DropTownService, DropTownStandar } from '../../../service/droptown.service';

interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Component({
  selector: 'app-atributo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './atributo.component.html',
  styleUrl: './atributo.component.css'
})
export class AtributoComponent implements OnInit {
  atributos: AtributoListItem[] = [];
  pageInfo: PageInfo = {
    page: 0,
    size: 15,
    totalElements: 0,
    totalPages: 0
  };
saving = false;  // ← Agrega esta línea en la clase
  // Formulario modal
  isModalOpen = false;
  isEditMode = false;
  currentAtributoId: number | null = null;
  nombreAtributo = '';
// Dentro de la clase AtributoComponent
tiendas: DropTownStandar[] = [];
selectedTiendaId: number | null = null;
loadingTiendas = false;
  // Búsqueda
  searchTerm = '';
  loading = false;

  constructor(
    private atributoService: AtributoAdminService,
    public auth: AuthService,
    private dropTownService: DropTownService
  ) {}

  ngOnInit(): void {
    
    this.cargarAtributos();
    // Solo cargar tiendas si es admin
  if (this.auth.isAdmin()) {
    this.loadingTiendas = true;
    this.dropTownService.getTiendas().subscribe({
      next: (tiendas) => {
        this.tiendas = tiendas;
        this.loadingTiendas = false;
      },
      error: () => {
        this.loadingTiendas = false;
        Swal.fire('Error', 'No se pudieron cargar las tiendas.', 'error');
      }
    });
  }
  }

  cargarAtributos(page: number = 0): void {
    this.loading = true;

    if (this.auth.isAdmin()) {
      // ADMIN: listado paginado desde el backend
      this.atributoService.listar(page, this.pageInfo.size, 'nombre', 'asc')
        .subscribe({
          next: (response: PageResponse<AtributoListItem>) => {
            this.atributos = response.content || [];
            this.pageInfo = {
              page: response.number ?? 0,
              size: response.size ?? 15,
              totalElements: response.totalElements ?? 0,
              totalPages: response.totalPages ?? 0
            };
            this.loading = false;
          },
          error: (err) => this.handleError(err)
        });
    } else {
      // OWNER: lista simple sin paginación
      this.atributoService.listarSimples()
        .subscribe({
          next: (atributos: AtributoListItem[]) => {
            this.atributos = atributos || [];
            this.pageInfo = {
              page: 0,
              size: atributos.length,
              totalElements: atributos.length,
              totalPages: 1
            };
            this.loading = false;
          },
          error: (err) => this.handleError(err)
        });
    }
  }

  private handleError(err: any): void {
    console.error('Error en atributos:', err);
    this.atributos = [];
    this.pageInfo = { page: 0, size: 15, totalElements: 0, totalPages: 0 };
    this.loading = false;

    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: err.error?.message || 'No se pudieron cargar los atributos.',
      confirmButtonText: 'Aceptar'
    });
  }

  buscar(): void {
    if (!this.searchTerm.trim()) {
      this.cargarAtributos(this.pageInfo.page);
      return;
    }

    // Búsqueda local (cliente)
    this.atributos = this.atributos.filter(a =>
      a.nombre.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  limpiarBusqueda(): void {
    this.searchTerm = '';
    this.cargarAtributos(this.pageInfo.page);
  }

  cambiarPagina(page: number): void {
    if (page >= 0 && page < this.pageInfo.totalPages && page !== this.pageInfo.page) {
      this.cargarAtributos(page);
    }
  }

  abrirModalCrear(): void {
    this.isEditMode = false;
    this.currentAtributoId = null;
    this.nombreAtributo = '';
    this.selectedTiendaId = null;
    this.isModalOpen = true;
  }

  abrirModalEditar(atributo: AtributoListItem): void {
    this.isEditMode = true;
    this.currentAtributoId = atributo.id;
    this.nombreAtributo = atributo.nombre;
    this.selectedTiendaId = null;
    this.isModalOpen = true;
  }

  cerrarModal(): void {
    this.isModalOpen = false;
    this.nombreAtributo = '';
    this.currentAtributoId = null;
  }

guardarAtributo(): void {
  if (!this.nombreAtributo.trim()) {
    Swal.fire({
      icon: 'warning',
      title: 'Campo requerido',
      text: 'El nombre del atributo es obligatorio.',
      confirmButtonText: 'Aceptar'
    });
    return;
  }

  // Validación adicional para admin al crear
  if (this.auth.isAdmin() && !this.isEditMode && !this.selectedTiendaId) {
    Swal.fire({
      icon: 'warning',
      title: 'Selecciona una tienda',
      text: 'Como administrador, debes elegir la tienda para este atributo.',
      confirmButtonText: 'Aceptar'
    });
    return;
  }

  this.saving = true;

  const request: AtributoCreateRequest | AtributoUpdateRequest = {
    nombre: this.nombreAtributo.trim(),
    // Solo enviar tiendaId si es admin y está creando
    ...(this.auth.isAdmin() && !this.isEditMode && this.selectedTiendaId ? { tiendaId: this.selectedTiendaId } : {})
  };

  const observable = this.isEditMode && this.currentAtributoId
    ? this.atributoService.actualizar(this.currentAtributoId, request)
    : this.atributoService.crear(request);

  observable.subscribe({
    next: () => {
      this.saving = false;
      Swal.fire({
        icon: 'success',
        title: '¡Éxito!',
        text: `Atributo ${this.isEditMode ? 'actualizado' : 'creado'} correctamente.`,
        timer: 2000,
        showConfirmButton: false
      });
      this.cerrarModal();
      this.cargarAtributos(this.pageInfo.page);
    },
    error: (err) => {
      this.saving = false;
      const mensaje = err.error?.message || 'Error al guardar el atributo.';
      Swal.fire({
        icon: 'error',
        title: 'Error',
        text: mensaje,
        confirmButtonText: 'Aceptar'
      });
    }
  });
}

  eliminarAtributo(id: number, nombre: string): void {
    Swal.fire({
      title: '¿Eliminar atributo?',
      text: `Se eliminará permanentemente "${nombre}". Esta acción no se puede deshacer.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.atributoService.eliminar(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Eliminado',
              text: 'El atributo ha sido eliminado correctamente.',
              timer: 2000,
              showConfirmButton: false
            });
            this.cargarAtributos(this.pageInfo.page);
          },
          error: () => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo eliminar el atributo. Puede estar en uso.',
              confirmButtonText: 'Aceptar'
            });
          }
        });
      }
    });
  }
}