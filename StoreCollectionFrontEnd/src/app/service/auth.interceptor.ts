// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // 1. Agregar token si existe (excepto en login/register)
  if (token && 
      !req.url.includes('/api/auth/login') && 
      !req.url.includes('/api/auth/register')) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 2. Manejar errores globales (excluyendo login/register)
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Solo manejar 401/403 en rutas protegidas (NO en login/register)
      if ((error.status === 401 || error.status === 403) && 
          !req.url.includes('/api/auth/login') && 
          !req.url.includes('/api/auth/register')) {

        if (error.status === 401) {
          // Sesión realmente expirada (token inválido)
          authService.logout();
          alert('Tu sesión ha expirado. Por favor inicia sesión nuevamente.');
          router.navigate(['/login']);
        }

        if (error.status === 403) {
          const mensajeBackend = error.error?.error || error.error?.message || '';

          const esProblemaSuscripcion = mensajeBackend.toLowerCase().includes('plan') ||
                                       mensajeBackend.toLowerCase().includes('vigente') ||
                                       mensajeBackend.toLowerCase().includes('inactiva') ||
                                       mensajeBackend.toLowerCase().includes('suscripción') ||
                                       mensajeBackend.toLowerCase().includes('tienda') ||
                                       mensajeBackend.toLowerCase().includes('activo');

          if (esProblemaSuscripcion) {
            router.navigate(['/suscripcion-expirada']);
          } else {
            alert(mensajeBackend || 'No tienes permisos para realizar esta acción.');
          }
        }
      }

      // Re-lanzamos el error para que el componente (ej: login) lo maneje con SweetAlert
      return throwError(() => error);
    })
  );
};