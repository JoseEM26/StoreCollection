import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../auth/auth.service';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email = 'admin@storecollection.com';
  password = '123456';
  isLoading = false;
  showCredentials = false;

  get currentYear(): number {
    return new Date().getFullYear();
  }

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  login() {
    if (!this.email || !this.password) return;

    this.isLoading = true;

    setTimeout(() => {
      if (this.auth.login(this.email.trim(), this.password)) {
        this.router.navigate(['/admin/dashboard']);
      } else {
        alert('Credenciales incorrectas');
        this.isLoading = false;
      }
    }, 800);
  }

  toggleCredentials() {
    this.showCredentials = !this.showCredentials;
  }
}