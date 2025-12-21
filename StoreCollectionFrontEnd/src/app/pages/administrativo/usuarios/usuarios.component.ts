// src/app/pages/administrativo/usuarios/usuarios.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioAdminService } from '../../../service/service-admin/usuario-admin.service';
import { UsuarioPage, UsuarioResponse } from '../../../model/admin/usuario-admin.model';
import { UsuariosFormComponent } from './usuarios-form/usuarios-form.component';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, UsuariosFormComponent],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent implements OnInit {
  pageData = signal<UsuarioPage | null>(null);
  usuarios = signal<UsuarioResponse[]>([]);
  loading = signal(true);

  currentPage = signal(0);
  pageSize = 10;
  searchTerm = '';

  // Modal
  showModal = signal(false);
  editingUsuario = signal<UsuarioResponse | null>(null);

  // Debounce para búsqueda
  private searchTimeout: any;

  // Para calcular el rango de paginación
  get startItem(): number {
    return this.currentPage() * this.pageSize + 1;
  }

  get endItem(): number {
    const calculated = (this.currentPage() + 1) * this.pageSize;
    const total = this.pageData()?.totalElements || 0;
    return calculated > total ? total : calculated;
  }

  constructor(private usuarioService: UsuarioAdminService) {
    effect(() => this.loadUsuarios());
  }

  ngOnInit(): void {
    this.loadUsuarios();
  }

  loadUsuarios(): void {
    this.loading.set(true);
    this.usuarioService.listarUsuarios(this.currentPage(), this.pageSize, this.searchTerm)
      .subscribe({
        next: (data) => {
          this.pageData.set(data);
          this.usuarios.set(data.content);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          alert('Error al cargar usuarios');
        }
      });
  }

  abrirCrear(): void {
    this.editingUsuario.set(null);
    this.showModal.set(true);
  }

  abrirEditar(usuario: UsuarioResponse): void {
    this.editingUsuario.set(usuario);
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingUsuario.set(null);
  }

  onFormSuccess(savedUsuario: UsuarioResponse): void {
    this.closeModal();
    this.loadUsuarios();
  }

  toggleActivo(usuario: UsuarioResponse): void {
    if (usuario.rol === 'ADMIN') {
      alert('No se permite cambiar el estado de un usuario ADMINISTRADOR');
      return;
    }

    this.usuarioService.toggleActivo(usuario.id).subscribe({
      next: (updated) => {
        this.usuarios.update(list =>
          list.map(u => u.id === updated.id ? updated : u)
        );
      },
      error: () => alert('Error al cambiar el estado del usuario')
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
      this.currentPage.set(page);
      this.loadUsuarios();
    }
  }

  onSearch(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage.set(0);
      this.loadUsuarios();
    }, 500); // Debounce de 500ms
  }

  getRoleBadgeClass(rol: string): string {
    switch (rol) {
      case 'ADMIN': return 'bg-danger';
      case 'OWNER': return 'bg-primary';
      case 'CUSTOMER': return 'bg-success';
      default: return 'bg-secondary';
    }
  }

  getEstadoBadgeClass(activo: boolean): string {
    return activo ? 'bg-success' : 'bg-warning text-dark';
  }

  getPageNumbers(): number[] {
    const total = this.pageData()?.totalPages || 0;
    const current = this.currentPage();
    const delta = 2;
    const range = [];
    for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  trackById(index: number, usuario: UsuarioResponse): number {
    return usuario.id!;
  }

  // Para deshabilitar toggle en ADMIN
  canToggle(usuario: UsuarioResponse): boolean {
    return usuario.rol !== 'ADMIN';
  }
}