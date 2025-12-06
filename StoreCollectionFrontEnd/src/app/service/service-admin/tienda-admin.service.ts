// src/app/services/tienda-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import { TiendaPage } from '../../model/tienda-public.model';

@Injectable({
  providedIn: 'root'
})
export class TiendaAdminService {
  private apiUrl = `${environment.apiUrl}/api/owner/tiendas/admin-list`;

  constructor(private http: HttpClient) {}

  /**
   * Lista todas las tiendas (ADMIN) o solo las del usuario (OWNER)
   * El backend ya filtra según el rol del usuario autenticado
   */
  listarTiendas(
    page: number = 0,
    size: number = 12,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<TiendaPage> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<TiendaPage>(this.apiUrl, { params });
  }

  // Opcional: método para recargar con nuevos filtros
  buscarTiendas(filtros: {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
  } = {}): Observable<TiendaPage> {
    return this.listarTiendas(
      filtros.page ?? 0,
      filtros.size ?? 12,
      filtros.sort ?? 'nombre,asc',
      filtros.search
    );
  }
}