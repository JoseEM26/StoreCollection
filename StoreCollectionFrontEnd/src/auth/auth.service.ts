// src/app/core/services/auth.service.ts
import { Injectable, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../environment';

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { nombre: string; email: string; password: string; celular: string; }

export interface AuthResponse {
  token: string;
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}
export interface ErrorResponse {
  code: string;
  message: string;
}
export interface CurrentUser {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl + '/api/auth';

  private currentUserSignal = signal<CurrentUser | null>(null);
  public currentUser = this.currentUserSignal.asReadonly();

  // === ESTADO DE AUTENTICACIÓN ===
  public isLoggedIn = computed(() => this.currentUser() !== null);
  public isAdmin = computed(() => this.currentUser()?.rol === 'ADMIN');
  public isOwner = computed(() => this.currentUser()?.rol === 'OWNER');
  public isCustomer = computed(() => this.currentUser()?.rol === 'CUSTOMER');

  // === DATOS DEL USUARIO (100% seguros, sin errores de TS) ===
  public fullName = computed(() => this.currentUser()?.nombre || 'Usuario Anónimo');
  public firstName = computed(() => {
    const name = this.currentUser()?.nombre;
    return name ? name.trim().split(' ')[0] : 'Usuario';
  });

  public initials = computed(() => {
    const name = this.currentUser()?.nombre;
    if (!name) return 'US';
    const parts = name.trim().split(' ').filter(p => p.length > 0);
    const first = parts[0][0];
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase() || 'US';
  });

  public roleDisplay = computed(() => {
    const rol = this.currentUser()?.rol;
    switch (rol) {
      case 'ADMIN': return 'Administrador';
      case 'OWNER': return 'Propietario';
      case 'CUSTOMER': return 'Cliente';
      default: return 'Usuario';
    }
  });

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();

    // Sincroniza con localStorage (útil si hay múltiples pestañas)
    effect(() => {
      const user = this.currentUser();
      if (user) {
        localStorage.setItem('currentUser', JSON.stringify(user));
      } else {
        localStorage.removeItem('currentUser');
      }
    });
  }

 login(credentials: LoginRequest) {
  return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
    tap(res => this.setSession(res))
  );
}

  register(data: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap(res => this.setSession(res))
    );
  }

  private setSession(authResult: AuthResponse) {
    localStorage.setItem('token', authResult.token);
    const user: CurrentUser = {
      id: authResult.id,
      nombre: authResult.nombre,
      email: authResult.email,
      rol: authResult.rol
    };
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
    if (!token || !userStr) return;

    try {
      const user = JSON.parse(userStr) as CurrentUser;
      if (this.isTokenExpired(token)) {
        this.logout();
        return;
      }
      this.currentUserSignal.set(user);
    } catch {
      this.logout();
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  getAuthHeaders() {
    const token = this.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }
}