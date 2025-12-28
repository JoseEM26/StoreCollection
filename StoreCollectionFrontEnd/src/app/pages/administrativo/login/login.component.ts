// src/app/auth/login/login.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, LoginRequest, ErrorResponse } from '../../../../auth/auth.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  credentials: LoginRequest = { email: '', password: '' };
  showPassword = false;
  isLoading = false;

  currentYear = new Date().getFullYear();

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    // Validación básica frontend
    if (!this.credentials.email || !this.credentials.password) {
      Swal.fire({
        icon: 'warning',
        title: 'Campos incompletos',
        text: 'Por favor ingresa tu correo electrónico y contraseña',
        confirmButtonColor: '#3085d6',
        confirmButtonText: 'Entendido'
      });
      return;
    }

    this.isLoading = true;

    this.authService.login(this.credentials).subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: '¡Bienvenido!',
          text: 'Has iniciado sesión correctamente',
          timer: 1500,
          showConfirmButton: false,
          toast: true,
          position: 'top-end'
        });
        this.router.navigate(['/admin/dashboard']);
      },
      error: (err) => {
        const errorData: ErrorResponse | undefined = err.error; // ← Leemos el JSON del backend
        let title = 'Error al iniciar sesión';
        let text = 'Ocurrió un problema inesperado. Por favor intenta nuevamente.';
        let icon: 'error' | 'warning' | 'info' = 'error';

        // Mapeamos los códigos/mensajes del backend a alertas amigables
        if (errorData?.code === 'Cuenta desactivada' || errorData?.message?.includes('desactivada') || errorData?.message?.includes('inactiva')) {
          title = 'Cuenta desactivada';
          text = errorData.message || 'Tu cuenta está desactivada/inactiva. Contacta al administrador para reactivarla.';
          icon = 'warning';
        } 
        else if (errorData?.code === 'Credenciales inválidas' || errorData?.message?.includes('credenciales') || errorData?.message?.includes('incorrectos')) {
          title = 'Credenciales incorrectas';
          text = errorData.message || 'El correo electrónico o la contraseña son incorrectos. Verifica e intenta nuevamente.';
          icon = 'error';
        } 
        else if (errorData?.code === 'Usuario no encontrado' || errorData?.message?.includes('no encontrado')) {
          title = 'Usuario no encontrado';
          text = errorData.message || 'No existe una cuenta registrada con ese correo electrónico.';
          icon = 'info';
        } 
        else if (err.status === 0) {
          title = 'Sin conexión';
          text = 'No pudimos conectar con el servidor. Verifica tu conexión a internet.';
          icon = 'error';
        }

        Swal.fire({
          icon: icon,
          title: title,
          text: text,
          confirmButtonColor: '#3085d6',
          confirmButtonText: 'Entendido'
        });

        this.isLoading = false;
      }
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }
}