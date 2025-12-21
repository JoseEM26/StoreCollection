// src/app/services/categoria-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { catchError, map, Observable, of } from 'rxjs';
import { Categoria } from '../model';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class CategoriaPublicService {private apiUrl = `${environment.apiUrl}/api/public/tiendas`;
  constructor(private http: HttpClient, private tiendaService: TiendaService) {}

  getAll(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.tiendaService.getBaseUrl()}/categorias`);
  }
  isCategoriaActiva(tiendaSlug: string, categoriaSlug: string): Observable<boolean> {
    // Como no tienes endpoint individual por slug de categoría,
    // usamos el listado y buscamos si aparece
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