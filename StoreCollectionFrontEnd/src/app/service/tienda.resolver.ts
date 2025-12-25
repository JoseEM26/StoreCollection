// src/app/service/tienda.resolver.ts
import { Injectable } from '@angular/core';
import {
  Resolve,
  ActivatedRouteSnapshot,
  Router
} from '@angular/router';
import { TiendaService } from './tienda.service';
import { catchError, of } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TiendaResolver implements Resolve<any> {

  constructor(
    private tiendaService: TiendaService,
    private router: Router
  ) {}

  resolve(route: ActivatedRouteSnapshot): any {
    const slug = route.paramMap.get('tiendaSlug');

    if (!slug) {
      this.router.navigate(['/']);
      return of(null);
    }

    // Importante: Cargamos los datos de la tienda y los inyectamos en el service
    return this.tiendaService.cargarTiendaPorSlug(slug).pipe(
      map(tienda => {
        if (tienda) {
          // Actualizamos el BehaviorSubject con los datos reales
          this.tiendaService.actualizarTienda(tienda);
          return tienda;
        } else {
          // Si no existe la tienda, redirigimos al home o página de error
          this.router.navigate(['/']);
          return null;
        }
      }),
      catchError(() => {
        this.router.navigate(['/']);
        return of(null);
      })
    );
  }
}