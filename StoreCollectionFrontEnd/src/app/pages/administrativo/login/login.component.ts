// src/app/auth/login/login.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, LoginRequest } from '../../../../auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  // Modelo para el formulario
  credentials: LoginRequest = { email: '', password: '' };
showPassword = false;
  // Estado del login
  isLoading = false;
  showCredentials = false;

 

  currentYear = new Date().getFullYear();

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    if (!this.credentials.email || !this.credentials.password) {
      alert('Por favor completa todos los campos');
      return;
    }

    this.isLoading = true;

    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.router.navigate(['/admin/dashboard']);
      },
      error: (err) => {
        console.error('Error de login:', err);
        alert('Credenciales incorrectas o error del servidor');
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  toggleCredentials() {
    this.showCredentials = !this.showCredentials;
  }

}