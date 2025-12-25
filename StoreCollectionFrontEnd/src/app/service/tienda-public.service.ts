// src/app/service/tienda-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Tienda } from '../model';
import { TiendaPage } from '../model/tienda-public.model';
import { environment } from '../../../environment';

@Injectable({
  providedIn: 'root'
})
export class TiendaPublicService {
  private readonly apiUrl = `${environment.apiUrl}/api/public/tiendas`;

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  // Lista todas las tiendas (paginado)
  getAllTiendas(
    page = 0,
    size = 12,
    sort = 'nombre,asc',
    search?: string
  ): Observable<TiendaPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<TiendaPage>(this.apiUrl, { params });
  }

  // Obtiene una tienda por slug (sin actualizar el estado global)
  getTiendaBySlug(slug: string): Observable<Tienda | null> {
    return this.http.get<Tienda>(`${this.apiUrl}/${slug}`).pipe(
      catchError(() => of(null))
    );
  }

  // Carga la tienda actual usando el slug guardado y actualiza el estado global
  cargarTiendaActual(): Observable<Tienda | null> {
    const url = this.tiendaService.getBaseUrl();
    if (!url) {
      return of(null);
    }

    return this.http.get<Tienda>(url).pipe(
      tap(tienda => this.tiendaService.setTienda(tienda)),
      catchError(() => {
        this.tiendaService.setTienda(null);
        return of(null);
      })
    );
  }
}