// src/app/services/tienda-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environment';
import { TiendaPage } from '../../model/tienda-public.model';
import { TiendaResponse } from '../../model/admin/tienda-admin.model';

// src/app/service/service-admin/tienda-admin.service.ts (o donde tengas las interfaces)

export interface TiendaCreateRequest {
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda?: 'SOLES' | 'DOLARES';        // Opcional en creación (puede tener valor por defecto)
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapa_url?: string;
  logo_img_url?: string;
  planId?: number;
  userId?: number;                     
  activo?: boolean;                    
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
  logo_img_url?: string;
  planId?: number | null;
  activo?: boolean;                   
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

  // === OBTENER UNA TIENDA POR ID (con verificación de permisos en backend) ===
  obtenerTiendaPorId(id: number): Observable<TiendaResponse> {
    return this.http.get<TiendaResponse>(`${this.BASE_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // === CREAR TIENDA ===
  crearTienda(request: TiendaCreateRequest): Observable<TiendaResponse> {
    return this.http.post<TiendaResponse>(this.BASE_URL, request)
      .pipe(catchError(this.handleError));
  }

  // === ACTUALIZAR TIENDA ===
  actualizarTienda(id: number, request: TiendaUpdateRequest): Observable<TiendaResponse> {
    return this.http.put<TiendaResponse>(`${this.BASE_URL}/${id}`, request)
      .pipe(catchError(this.handleError));
  }

  // === ELIMINAR TIENDA (opcional) ===
  eliminarTienda(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // === GENERAR SLUG AUTOMÁTICO (helper en frontend) ===
  generarSlug(nombre: string): string {
    return nombre
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')           // Quita caracteres especiales
      .replace(/\s+/g, '-')                   // Espacios → guiones
      .replace(/-+/g, '-')                    // Evita guiones duplicados
      .substring(0, 50);                      // Máx 50 caracteres
  }

  // === MANEJO DE ERRORES ===
  private handleError(error: HttpErrorResponse) {
    let mensaje = 'Ocurrió un error inesperado';

    if (error.error instanceof ErrorEvent) {
      // Error del cliente
      mensaje = error.error.message;
    } else {
      // Error del servidor
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