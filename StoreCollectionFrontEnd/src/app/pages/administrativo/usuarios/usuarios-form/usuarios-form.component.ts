// usuarios-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { UsuarioRequest, UsuarioResponse } from '../../../../model/admin/usuario-admin.model';
import { UsuarioAdminService } from '../../../../service/service-admin/usuario-admin.service';

@Component({
  selector: 'app-usuarios-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuarios-form.component.html',
  styleUrl: './usuarios-form.component.css'
})
export class UsuariosFormComponent implements OnInit {
  @Input() usuario: UsuarioResponse | null = null;
  @Output() success = new EventEmitter<UsuarioResponse>();
  @Output() cancel = new EventEmitter<void>();

  esEdicion = false;
  loading = false; // ← Variable booleana simple (recomendada)

  form: UsuarioRequest = {
    nombre: '',
    email: '',
    password: '',
    celular: '',
    activo: true,
    rol: 'CUSTOMER'
  };

  constructor(private usuarioService: UsuarioAdminService) {}

  ngOnInit(): void {
    if (this.usuario) {
      this.esEdicion = true;
      this.form = {
        nombre: this.usuario.nombre,
        email: this.usuario.email,
        password: '', // Nunca precargamos contraseña
        celular: this.usuario.celular || '',
        activo: this.usuario.activo,
        rol: this.usuario.rol
      };
    } else {
      this.esEdicion = false;
      this.resetForm();
    }
  }

  resetForm(): void {
    this.form = {
      nombre: '',
      email: '',
      password: '',
      celular: '',
      activo: true,
      rol: 'CUSTOMER'
    };
  }

  guardar(): void {
    if (!this.form.nombre || !this.form.email || (!this.esEdicion && !this.form.password)) {
      alert('Completa los campos obligatorios');
      return;
    }

    this.loading = true; // ← Activamos loading

    const obs = this.esEdicion && this.usuario
      ? this.usuarioService.actualizar(this.usuario.id, this.form)
      : this.usuarioService.crear(this.form);

    obs.subscribe({
      next: (saved) => {
        this.success.emit(saved);
        this.loading = false; // ← Desactivamos al terminar
      },
      error: (err) => {
        console.error('Error al guardar usuario:', err);
        alert('Error al guardar el usuario');
        this.loading = false; // ← Importante: siempre desactivar
      }
    });
  }

  cancelar(): void {
    this.cancel.emit();
  }
}