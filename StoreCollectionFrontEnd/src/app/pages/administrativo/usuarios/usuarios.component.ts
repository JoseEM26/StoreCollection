// usuarios.component.ts
import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: 'OWNER' | 'ADMIN' | 'CLIENTE';
  activo: boolean;
  creadoEn: string;
}

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css'
})
export class UsuariosComponent {
  usuarios: Usuario[] = [
    { id: 1, nombre: 'Juan Pérez', email: 'juan@store.com', rol: 'OWNER', activo: true, creadoEn: '2024-01-15' },
    { id: 2, nombre: 'María Gómez', email: 'maria@store.com', rol: 'ADMIN', activo: true, creadoEn: '2024-03-20' },
    { id: 3, nombre: 'Carlos López', email: 'carlos@gmail.com', rol: 'CLIENTE', activo: true, creadoEn: '2025-01-10' },
    { id: 4, nombre: 'Ana Torres', email: 'ana@store.com', rol: 'ADMIN', activo: false, creadoEn: '2024-08-05' },
  ];

  busqueda = '';
  filtroRol: string = 'TODOS';
  modalAbierto = false;
  esEdicion = false;
  usuarioTemporal: Partial<Usuario> = {};

  get usuariosFiltrados(): Usuario[] {
    return this.usuarios.filter(u => {
      const porRol = this.filtroRol === 'TODOS' || u.rol === this.filtroRol;
      const porTexto = !this.busqueda || 
        u.nombre.toLowerCase().includes(this.busqueda.toLowerCase()) ||
        u.email.toLowerCase().includes(this.busqueda.toLowerCase());
      return porRol && porTexto;
    });
  }

  abrirModalCrear() {
    this.esEdicion = false;
    this.usuarioTemporal = { nombre: '', email: '', rol: 'CLIENTE', activo: true };
    this.modalAbierto = true;
  }

  abrirModalEditar(usuario: Usuario) {
    this.esEdicion = true;
    this.usuarioTemporal = { ...usuario };
    this.modalAbierto = true;
  }

  cerrarModal() {
    this.modalAbierto = false;
  }

  guardarUsuario() {
    if (!this.usuarioTemporal.nombre || !this.usuarioTemporal.email) return;

    if (this.esEdicion) {
      const index = this.usuarios.findIndex(u => u.id === (this.usuarioTemporal as Usuario).id);
      if (index !== -1) {
        this.usuarios[index] = { ...this.usuarios[index], ...(this.usuarioTemporal as Usuario) };
      }
    } else {
      const nuevo: Usuario = {
        id: Math.max(...this.usuarios.map(u => u.id), 0) + 1,
        nombre: this.usuarioTemporal.nombre!,
        email: this.usuarioTemporal.email!,
        rol: this.usuarioTemporal.rol as 'OWNER' | 'ADMIN' | 'CLIENTE',
        activo: this.usuarioTemporal.activo ?? true,
        creadoEn: new Date().toISOString().split('T')[0]
      };
      this.usuarios.unshift(nuevo);
    }

    this.cerrarModal();
  }

  eliminarUsuario(usuario: Usuario) {
    if (confirm(`¿Eliminar permanentemente a ${usuario.nombre}?`)) {
      this.usuarios = this.usuarios.filter(u => u.id !== usuario.id);
    }
  }

  toggleEstado(usuario: Usuario) {
    usuario.activo = !usuario.activo;
  }
}