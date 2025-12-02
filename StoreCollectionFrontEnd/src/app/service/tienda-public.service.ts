// src/app/services/tienda-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable } from 'rxjs';
import { Tienda } from '../model';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TiendaPublicService {
  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  cargarTiendaActual(): Observable<Tienda> {
    const url = this.tiendaService.getBaseUrl();
    return this.http.get<Tienda>(url).pipe(
      tap(tienda => this.tiendaService.setTienda(tienda))
    );
  }
}