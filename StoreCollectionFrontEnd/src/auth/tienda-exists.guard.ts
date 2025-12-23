// src/app/auth/tienda-exists.guard.ts (o donde lo tengas)

import { inject } from '@angular/core';
import { CanMatchFn, Router, Route, UrlSegment } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { TiendaPublicService } from '../app/service/tienda-public.service';

// Lista de rutas estáticas que NO deben ser tratadas como slug de tienda
const RUTAS_IGNORADAS = [
  'login',
  'admin',
  'suscripcion-expirada',
  // Puedes agregar más si crecen: 'terminos', 'privacidad', etc.
];

export const tiendaExistsGuard: CanMatchFn = (route: Route, segments: UrlSegment[]) => {
  const router = inject(Router);
  const tiendaService = inject(TiendaPublicService);

  // Extraemos el primer segmento (el que sería el :tiendaSlug)
  const possibleSlug = segments[0]?.path;

  if (!possibleSlug) {
    return true;
  }

  // Si el segmento coincide con una ruta estática conocida → NO aplicar el guard
  if (RUTAS_IGNORADAS.includes(possibleSlug)) {
    return false; // ← Importante: false significa "esta ruta NO coincide con :tiendaSlug"
  }

  // Si no es ruta ignorada → verificar si existe la tienda
  return tiendaService.getTiendaBySlug(possibleSlug).pipe(
    map(tienda => !!tienda),
    catchError(() => of(false)),
    map(exists => {
      if (exists) {
        return true; // Sí existe → permitir acceso a la tienda
      } else {
        // No existe → redirigir al home
        return router.createUrlTree(['/']);
      }
    })
  );
};