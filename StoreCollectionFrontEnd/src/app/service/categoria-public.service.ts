import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { catchError, map, Observable, of } from 'rxjs';
import { Categoria } from '../model';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class CategoriaPublicService {

  constructor(private http: HttpClient, private tiendaService: TiendaService) {}

  // Obtiene todas las categorías de la tienda actual
  getAll(): Observable<Categoria[]> {
    const tiendaSlug = this.tiendaService.getBaseUrl(); // obtenemos slug
    if (!tiendaSlug) return of([]);
    return this.http.get<Categoria[]>(`${environment.apiUrl}/api/public/tiendas/${tiendaSlug}/categorias`).pipe(
      catchError(() => of([]))
    );
  }

 isCategoriaActiva(tiendaSlug: string, categoriaSlug: string): Observable<boolean> {
    // Como no tienes endpoint individual por slug de categoría,
    // usamos el listado y buscamos si aparece
    return this.http.get<any[]>(`${environment.apiUrl}/api/public/tiendas/${tiendaSlug}/categorias`).pipe(
      map(categorias => {
        return categorias.some(cat => 
          cat.slug === categoriaSlug && cat.activo === true
        );
      }),
      catchError(() => of(false))
    );
  }
}
