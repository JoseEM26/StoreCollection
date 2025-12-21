// src/app/auth/resource-active.guard.ts

import { inject } from '@angular/core';
import { CanMatchFn, Router, Route, UrlSegment } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { ProductoPublicService } from '../app/service/producto-public.service';
import { CategoriaPublicService } from '../app/service/categoria-public.service';

export const resourceActiveGuard: CanMatchFn = (route: Route, segments: UrlSegment[]) => {
  const router = inject(Router);
  const productoService = inject(ProductoPublicService);
  const categoriaService = inject(CategoriaPublicService);

  // Ejemplo de estructura de URL:
  // segments: [ 'mi-tienda', 'productos', 'mi-producto-slug' ]
  //           [ 'mi-tienda', 'categorias', 'mi-categoria-slug' ]

  if (segments.length < 3) {
    return true; // No es una ruta de detalle → dejar pasar
  }

  const tiendaSlug = segments[0]?.path;
  const tipo = segments[1]?.path; // 'productos' o 'categorias'
  const recursoSlug = segments[2]?.path;

  if (!tiendaSlug || !tipo || !recursoSlug) {
    return true;
  }

  // Solo aplicar a rutas públicas de detalle
  if (tipo === 'productos') {
    return productoService.isProductoActivo(tiendaSlug, recursoSlug).pipe(
      map(activo => activo ? true : router.createUrlTree(['/', tiendaSlug])),
      catchError(() => of(router.createUrlTree(['/', tiendaSlug])))
    );
  }

  if (tipo === 'categorias') {
    return categoriaService.isCategoriaActiva(tiendaSlug, recursoSlug).pipe(
      map(activa => activa ? true : router.createUrlTree(['/', tiendaSlug])),
      catchError(() => of(router.createUrlTree(['/', tiendaSlug])))
    );
  }

  return true;
};