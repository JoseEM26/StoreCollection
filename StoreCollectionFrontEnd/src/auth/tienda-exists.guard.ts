import { inject } from '@angular/core';
import { CanMatchFn, Router, Route, UrlSegment } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { TiendaPublicService } from '../app/service/tienda-public.service';

export const tiendaExistsGuard: CanMatchFn = (route: Route, segments: UrlSegment[]) => {
  const router = inject(Router);
  const tiendaService = inject(TiendaPublicService);
  const slug = segments[0]?.path;

  if (!slug) return true;

  return tiendaService.getTiendaBySlug(slug).pipe(
    map(tienda => !!tienda), // Si existe → true
    catchError(() => of(false)), // Si falla → false
    map(exists => exists ? true : router.createUrlTree(['/'])) // ← Si no existe → va al home
  );
};