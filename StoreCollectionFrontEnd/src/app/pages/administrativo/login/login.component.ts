import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../auth/auth.service';

@Component({
    selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterModule], 
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email = 'admin@storecollection.com';
  password = '123456';
  isLoading = false;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  login() {
    this.isLoading = true;

    setTimeout(() => {
      if (this.auth.login(this.email, this.password)) {
        this.router.navigate(['/admin']);
      } else {
        alert('Credenciales incorrectas');
        this.isLoading = false;
      }
    }, 800);
  }
}