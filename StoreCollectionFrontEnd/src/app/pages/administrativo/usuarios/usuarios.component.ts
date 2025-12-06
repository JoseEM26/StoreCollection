// src/app/pages/administrativo/usuarios/usuarios.component.ts
import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioAdminService } from '../../../service/service-admin/usuario-admin.service';
import { UsuarioPage, UsuarioResponse } from '../../../model/admin/usuario-admin.model';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  constructor(
    private usuarioService: UsuarioAdminService,
    public auth: AuthService
  ) {
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

  goToPage(page: number): void {
    if (page >= 0 && page < (this.pageData()?.totalPages || 0)) {
      this.currentPage.set(page);
    }
  }

  onSearch(): void {
    this.currentPage.set(0);
  }

  getRoleBadgeClass(rol: string): string {
    switch (rol) {
      case 'ADMIN': return 'bg-danger';
      case 'OWNER': return 'bg-primary';
      case 'CUSTOMER': return 'bg-success';
      default: return 'bg-secondary';
    }
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
}