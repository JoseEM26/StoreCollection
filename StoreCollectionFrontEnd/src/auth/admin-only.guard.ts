// src/app/auth/admin-only.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const adminOnlyGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true; // Es ADMIN → puede pasar
  }

  // No es ADMIN (probablemente OWNER) → redirigir a dashboard
  router.navigate(['/admin/dashboard']);
  return false;
};