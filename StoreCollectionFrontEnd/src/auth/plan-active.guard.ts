import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import Swal from 'sweetalert2';
import { DashboardService } from '../app/service/service-admin/dashboard.service';

export const planActiveGuard: CanActivateFn = (route, state) => {
  const dashboardService = inject(DashboardService);
  const router = inject(Router);

  return dashboardService.getPlanUsage().pipe(
    map(usage => {
      if (!usage || usage.diasRestantesRenovacion <= 0) {
        // Plan vencido o no disponible → redirigir al dashboard + alerta
        Swal.fire({
          icon: 'warning',
          title: '¡Plan vencido!',
          text: 'Tu plan ha expirado. No puedes acceder a esta sección hasta renovar.',
          confirmButtonText: 'Ir al Dashboard',
          confirmButtonColor: '#3085d6',
          allowOutsideClick: false
        }).then(() => {
          router.navigate(['/admin/dashboard']);
        });

        return false; // Bloquea la ruta
      }

      // Plan activo → permite acceso
      return true;
    }),
    catchError(() => {
      // Error al cargar → por seguridad, redirigir
      router.navigate(['/admin/dashboard']);
      return of(false);
    })
  );
};