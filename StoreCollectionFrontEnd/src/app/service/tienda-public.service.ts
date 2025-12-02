// src/app/services/tienda-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable, tap } from 'rxjs';
import { Tienda } from '../model';

@Injectable({ providedIn: 'root' })
export class TiendaPublicService {

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService  // el que guarda el slug
  ) {}

  cargarTiendaActual(): Observable<Tienda> {
    const url = `${this.tiendaService.getBaseUrl()}`;  // → /api/public/tiendas/zapatik
    return this.http.get<Tienda>(url).pipe(
      tap(tienda => {
        // Guardamos los datos reales de la tienda para usar en header, hero, etc.
        (this.tiendaService as any).currentTiendaSubject?.next(tienda);
      })
    );
  }
}