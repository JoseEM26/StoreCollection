// src/app/service/service-admin/tienda-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environment';
import { TiendaPage, TiendaResponse } from '../../model/admin/tienda-admin.model';

export interface TiendaCreateRequest {
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda?: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapa_url?: string;
  logo_img_url?: string;
  planId?: number;
}

export type TiendaSaveRequest = TiendaCreateRequest & { id?: number };

@Injectable({
  providedIn: 'root'
})
export class TiendaAdminService {
  private readonly BASE_URL = `${environment.apiUrl}/api/owner/tiendas`;

  constructor(private http: HttpClient) {}

  // Un solo método: crea si no tiene id, actualiza si tiene
  guardarTienda(request: TiendaSaveRequest): Observable<TiendaResponse> {
    return this.http.post<TiendaResponse>(this.BASE_URL, request)
      .pipe(catchError(this.handleError));
  }

crearTienda(request: TiendaCreateRequest): Observable<TiendaResponse> {
  return this.http.post<TiendaResponse>(this.BASE_URL, request)
    .pipe(catchError(this.handleError));
}
actualizarTienda(id: number, request: TiendaCreateRequest): Observable<TiendaResponse> {
  return this.http.put<TiendaResponse>(`${this.BASE_URL}/${id}`, request)
    .pipe(catchError(this.handleError));
}
  listarTiendas(
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

    return this.http.get<TiendaPage>(`${this.BASE_URL}/admin-list`, { params })
      .pipe(catchError(this.handleError));
  }

  generarSlug(nombre: string): string {
    return nombre
      .toLowerCase()
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .substring(0, 60);
  }

  private handleError(error: HttpErrorResponse) {
    let mensaje = 'Ocurrió un error inesperado';
    if (error.status === 400) mensaje = 'Datos inválidos';
    else if (error.status === 403) mensaje = 'No tienes permiso';
    else if (error.status === 409) mensaje = 'El slug ya está en uso';
    else if (error.error?.message) mensaje = error.error.message;

    console.error('[TiendaAdminService]', error);
    return throwError(() => new Error(mensaje));
  }
}