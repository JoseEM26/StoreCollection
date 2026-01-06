// src/app/services/producto-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { catchError, map, Observable, of } from 'rxjs';
import { ProductoPublic } from '../model/index.dto';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class ProductoPublicService {
  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  getAll(): Observable<ProductoPublic[]> {
  const tiendaSlug = this.tiendaService.getBaseUrl(); // solo slug, ej: activewear-fit
  if (!tiendaSlug) return of([]);
  return this.http.get<ProductoPublic[]>(
    `${environment.apiUrl}${tiendaSlug}/productos`
  ).pipe(catchError(() => of([])));
}

  isProductoActivo(tiendaSlug: string, productoSlug: string): Observable<boolean> {
    return this.http.get<any>(`${environment.apiUrl}/api/public/tiendas/${tiendaSlug}/productos/${productoSlug}`).pipe(
      map(() => true), // Si llega respuesta → está activo
      catchError(() => of(false)) // 403, 404, etc → inactivo o no existe
    );
  }
  getBySlug(slug: string): Observable<ProductoPublic> {
    const base = this.tiendaService.getBaseUrl();
    return this.http.get<ProductoPublic>(`${environment.apiUrl}${base}/productos/${slug}`);
  }
}