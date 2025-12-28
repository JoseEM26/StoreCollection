// src/app/auth/tienda-access.guard.ts
import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlSegment } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { TiendaPublicService } from '../app/service/tienda-public.service';

// Rutas que NO deben ser consideradas como :tiendaSlug
const RUTAS_IGNORADAS = [
  'login',
  'admin',
  'suscripcion-expirada',
  'terminos',
  'privacidad',
  'contacto'
  // Agrega aquí todas las rutas estáticas que puedan coincidir con un slug
];

export const tiendaAccessGuard: CanMatchFn = (route, segments: UrlSegment[]) => {
  const router = inject(Router);
  const tiendaPublicService = inject(TiendaPublicService);

  const possibleSlug = segments[0]?.path;

  // Sin segmento o es ruta estática → NO aplica este guard (deja pasar a otras rutas)
  if (!possibleSlug || RUTAS_IGNORADAS.includes(possibleSlug.toLowerCase())) {
    return false;
  }

  return tiendaPublicService.getTiendaBySlug(possibleSlug).pipe(
    map(tienda => {
      // Caso 1: La tienda NO existe
      if (!tienda) {
        return router.createUrlTree(['/']);
      }

      // Caso 2: La tienda existe pero está desactivada
      if (!tienda.activo) {
        return router.createUrlTree(['/suscripcion-expirada'], {
          queryParams: { 
            slug: possibleSlug,
            motivo: 'inactiva' 
          }
        });
      }

      // Caso 3: Todo OK → permitir acceso
      return true;
    }),
    catchError(() => {
      // Cualquier error (404, 500, timeout, etc.) → tratamos como no existe
      return of(router.createUrlTree(['/']));
    })
  );
};