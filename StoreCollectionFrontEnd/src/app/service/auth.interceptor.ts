// src/app/core/interceptors/auth.interceptor.ts

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { Router } from '@angular/router';  // ← Importamos Router
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);  // ← Inyectamos Router
  const token = authService.getToken();

  // 1. Agregar token si existe
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 2. Manejar errores globales
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Sesión expirada
        authService.logout();
        alert('Tu sesión ha expirado. Por favor inicia sesión nuevamente.');
        router.navigate(['/login']);
      }

      if (error.status === 403) {
        const mensajeBackend = error.error?.error;

        // Mensajes comunes que indican problema de suscripción/plan/tienda
        const esProblemaSuscripcion = mensajeBackend && (
          mensajeBackend.includes('plan') ||
          mensajeBackend.includes('vigente') ||
          mensajeBackend.includes('inactiva') ||
          mensajeBackend.includes('suscripción') ||
          mensajeBackend.includes('Tienda') ||
          mensajeBackend.includes('activo')
        );

        if (esProblemaSuscripcion) {
          // Redirección automática a página de suscripción expirada
          router.navigate(['/suscripcion-expirada']);
        } else {
          // Otros 403 (permisos, etc.)
          alert(mensajeBackend || 'No tienes permisos para realizar esta acción.');
        }
      }

      // Re-lanzamos el error por si algún componente quiere manejarlo
      return throwError(() => error);
    })
  );
};