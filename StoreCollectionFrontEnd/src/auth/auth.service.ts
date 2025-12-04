// src/app/core/services/auth.service.ts

import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
  celular: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}

export interface CurrentUser {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';

  // Estado reactivo del usuario actual
  private currentUserSignal = signal<CurrentUser | null>(null);
  public currentUser = this.currentUserSignal.asReadonly();

  // Computed para roles rápidos
  public isLoggedIn = computed(() => this.currentUser() !== null);
  public isAdmin = computed(() => this.currentUser()?.rol === 'ADMIN');
  public isOwner = computed(() => this.currentUser()?.rol === 'OWNER');
  public isCustomer = computed(() => this.currentUser()?.rol === 'CUSTOMER');

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Al iniciar la app, cargar usuario desde localStorage si existe
    this.loadUserFromStorage();
  }

  login(credentials: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => this.setSession(response))
    );
  }

  register(data: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap(response => this.setSession(response))
    );
  }

  private setSession(authResult: AuthResponse) {
    localStorage.setItem('token', authResult.token);

    const user: CurrentUser = {
      id: authResult.id,
      nombre: authResult.nombre,
      email: authResult.email,
      rol: authResult.rol,
      token: authResult.token
    };

    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  private loadUserFromStorage() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('currentUser');

    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as CurrentUser;
        // Opcional: validar que el token no esté expirado
        if (this.isTokenExpired(token)) {
          this.logout();
          return;
        }
        this.currentUserSignal.set(user);
      } catch (e) {
        this.logout();
      }
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // Extra: leer datos directamente del JWT (útil si no confías en localStorage)
  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}