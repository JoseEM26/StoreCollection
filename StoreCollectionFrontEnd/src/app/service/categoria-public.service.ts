// src/app/services/categoria-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { catchError, map, Observable, of } from 'rxjs';
import { Categoria } from '../model';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class CategoriaPublicService {
  private apiUrl = `${environment.apiUrl}/api/public/tiendas`;

  constructor(private http: HttpClient, private tiendaService: TiendaService) {}

  getAll(): Observable<Categoria[]> {
    const base = this.tiendaService.getBaseUrl();
    if (!base) {
      return of([]);
    }
    // Usamos URL absoluta: backend + /categorias
    return this.http.get<Categoria[]>(`${environment.apiUrl}${base}/categorias`);
  }

  isCategoriaActiva(tiendaSlug: string, categoriaSlug: string): Observable<boolean> {
    // Este método ya usaba apiUrl correctamente (absoluta), lo dejamos igual
    return this.http.get<any[]>(`${this.apiUrl}/${tiendaSlug}/categorias`).pipe(
      map(categorias => {
        return categorias.some(cat => 
          cat.slug === categoriaSlug && cat.activo === true
        );
      }),
      catchError(() => of(false))
    );
  }
}