// src/app/service/tienda-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environment';
import { TiendaPublic, TiendaPublicPage } from '../model/admin/tienda-admin.model';

@Injectable({
  providedIn: 'root'
})
export class TiendaPublicService {
  private readonly apiUrl = `${environment.apiUrl}/api/public/tiendas`;

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}
getAllTiendas(
  page = 0,
  size = 12,
  sort = 'nombre,asc',
  search?: string
): Observable<TiendaPublicPage> {
  let params = new HttpParams()
    .set('page', page.toString())
    .set('size', size.toString())
    .set('sort', sort);

  if (search?.trim()) {
    params = params.set('search', search.trim());
  }

  return this.http.get<TiendaPublicPage>(this.apiUrl, { params }).pipe(
    catchError((error) => {
      console.error('Error al cargar tiendas públicas:', error);

      const emptyPage: TiendaPublicPage = {
        content: [],
        pageable: {
          sort: { sorted: false, unsorted: true, empty: true },
          pageNumber: page,
          pageSize: size,
          offset: 0,
          paged: true,
          unpaged: false
        },
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        numberOfElements: 0,
        size: size,
        number: page,
        empty: true,
        sort: { sorted: false, unsorted: true, empty: true }  // ← Campo agregado
      };

      return of(emptyPage);
    })
  );
}
  /**
   * Obtiene una tienda pública por slug
   * No afecta el estado global de la app
   */
  getTiendaBySlug(slug: string): Observable<TiendaPublic | null> {
    return this.http.get<TiendaPublic>(`${this.apiUrl}/${slug}`).pipe(
      catchError((error) => {
        console.error(`Error al cargar tienda con slug ${slug}:`, error);
        return of(null);
      })
    );
  }

cargarTiendaActual(): Observable<TiendaPublic | null> {
  const slug = this.tiendaService.getCurrentSlug(); // o como lo llames
  if (!slug) {
    return of(null);
  }

  return this.http.get<TiendaPublic>(`${environment.apiUrl}/api/public/tiendas/${slug}`).pipe(
    tap(tienda => {
      if (tienda) this.tiendaService.setTienda(tienda);
    }),
    catchError(error => {
      console.error('Error al cargar tienda actual:', error);
      this.tiendaService.setTienda(null);
      return of(null);
    })
  );
}
}