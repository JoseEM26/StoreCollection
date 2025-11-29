import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _currentUser = signal<string | null>(null);
  currentUser = this._currentUser.asReadonly();

  login(email: string, password: string): boolean {
    if (email === 'admin@storecollection.com' && password === '123456') {
      this._currentUser.set(email);
      localStorage.setItem('user', email);
      return true;
    }
    return false;
  }

  logout() {
    this._currentUser.set(null);
    localStorage.removeItem('user');
  }

// src/app/auth/auth.service.ts
isLoggedIn(): boolean {
  const user = localStorage.getItem('user');
  if (user) {
    this._currentUser.set(user);
  }
  return !!this.currentUser();
}
}