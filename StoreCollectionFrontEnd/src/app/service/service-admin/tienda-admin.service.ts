// src/app/services/tienda-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environment';
import { TiendaAdminPage, TiendaResponse } from '../../model/admin/tienda-admin.model';

// Interfaces para crear y actualizar (se mantienen igual, pero ahora compatibles)
export interface TiendaCreateRequest {
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda?: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapa_url?: string;              // ← snake_case para coincidir con backend
  planId?: number;
  userId?: number;
  activo?: boolean;
  emailRemitente?: string;           // opcional
  emailAppPassword?: string;
}

export interface TiendaUpdateRequest {
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda?: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapa_url?: string;
  planId?: number | null;
  activo?: boolean;
  emailRemitente?: string;           // opcional
  emailAppPassword?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TiendaAdminService {
  private readonly BASE_URL = `${environment.apiUrl}/api/owner/tiendas`;

  constructor(private http: HttpClient) {}

  // === LISTADO (ADMIN ve todas, OWNER solo las suyas) ===
  listarTiendas(
    page: number = 0,
    size: number = 12,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<TiendaAdminPage> {  // ← Corregido: usa TiendaAdminPage
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<TiendaAdminPage>(`${this.BASE_URL}/admin-list`, { params })
      .pipe(catchError(this.handleError));
  }

  // === OBTENER UNA TIENDA POR ID ===
  obtenerTiendaPorId(id: number): Observable<TiendaResponse> {
    return this.http.get<TiendaResponse>(`${this.BASE_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // === CREAR TIENDA ===
  crearTienda(data: TiendaCreateRequest, logoImg?: File): Observable<TiendaResponse> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (logoImg) {
      formData.append('logoImg', logoImg, logoImg.name);
    }
    return this.http.post<TiendaResponse>(this.BASE_URL, formData)
      .pipe(catchError(this.handleError));
  }

  // === ACTUALIZAR TIENDA ===
  actualizarTienda(id: number, data: TiendaUpdateRequest, logoImg?: File): Observable<TiendaResponse> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (logoImg) {
      formData.append('logoImg', logoImg, logoImg.name);
    }
    return this.http.put<TiendaResponse>(`${this.BASE_URL}/${id}`, formData)
      .pipe(catchError(this.handleError));
  }

  // === TOGGLE ACTIVO (SOLO ADMIN) ===
  toggleActivo(id: number): Observable<TiendaResponse> {
    const url = `${this.BASE_URL}/${id}/toggle-activo`;
    return this.http.patch<TiendaResponse>(url, {})
      .pipe(catchError(this.handleError));
  }

  // === GENERAR SLUG AUTOMÁTICO (helper en frontend) ===
  generarSlug(nombre: string): string {
    return nombre
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .substring(0, 50);
  }

  // === MANEJO DE ERRORES ===
  private handleError(error: HttpErrorResponse) {
    let mensaje = 'Ocurrió un error inesperado';

    if (error.error instanceof ErrorEvent) {
      mensaje = error.error.message;
    } else {
      if (error.status === 400) {
        mensaje = error.error?.message || 'Datos inválidos';
      } else if (error.status === 403) {
        mensaje = 'No tienes permiso para realizar esta acción';
      } else if (error.status === 404) {
        mensaje = 'Tienda no encontrada';
      } else if (error.status === 409) {
        mensaje = error.error?.message || 'El slug ya está en uso';
      } else {
        mensaje = error.error?.message || `Error ${error.status}`;
      }
    }

    console.error('Error en TiendaAdminService:', error);
    return throwError(() => new Error(mensaje));
  }
}