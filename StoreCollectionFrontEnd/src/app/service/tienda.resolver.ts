// src/app/service/tienda.resolver.ts
import { Injectable } from '@angular/core';
import {
  Resolve,
  ActivatedRouteSnapshot,
  Router
} from '@angular/router';
import { TiendaPublicService } from './tienda-public.service';
import { TiendaService } from './tienda.service';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TiendaResolver implements Resolve<any> {

  constructor(
    private tiendaService: TiendaService,
    private tiendaPublicService: TiendaPublicService,
    private router: Router
  ) {}

  resolve(route: ActivatedRouteSnapshot): any {
    const slug = route.paramMap.get('tiendaSlug');

    if (!slug) {
      this.router.navigate(['/']);
      return of(null);
    }

    // 1. Guardamos el slug inmediatamente
    this.tiendaService.setSlug(slug);

    // 2. Cargamos los datos completos de la tienda y los inyectamos en el service
    return this.tiendaPublicService.cargarTiendaActual().pipe(
      map(tienda => {
        if (tienda) {
          return tienda; // Resolver devuelve la tienda (opcional, para usar en snapshot si quieres)
        } else {
          // Tienda no encontrada o error → redirigir
          this.tiendaService.limpiarTienda();
          this.router.navigate(['/']);
          return null;
        }
      }),
      catchError(() => {
        this.tiendaService.limpiarTienda();
        this.router.navigate(['/']);
        return of(null);
      })
    );
  }
}