// src/app/services/producto-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { catchError, map, Observable, of } from 'rxjs';
import { ProductoPublic } from '../model/index.dto';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class ProductoPublicService {
  private apiUrl = `${environment.apiUrl}/api/public/tiendas`;

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  getAll(): Observable<ProductoPublic[]> {
    const base = this.tiendaService.getBaseUrl();
    if (!base) {
      return of([]);
    }
    // Usamos URL absoluta: backend completo + la ruta de la tienda + /productos
    return this.http.get<ProductoPublic[]>(`${environment.apiUrl}${base}/productos`);
  }

  isProductoActivo(tiendaSlug: string, productoSlug: string): Observable<boolean> {
    return this.http.get<any>(`${this.apiUrl}/${tiendaSlug}/productos/${productoSlug}`).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  getBySlug(slug: string): Observable<ProductoPublic> {
    const base = this.tiendaService.getBaseUrl();
    if (!base) {
      throw new Error('No hay tienda actual');
    }
    // URL absoluta
    return this.http.get<ProductoPublic>(`${environment.apiUrl}${base}/productos/${slug}`);
  }
}