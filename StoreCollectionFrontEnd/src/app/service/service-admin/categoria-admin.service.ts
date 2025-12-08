// src/app/service/service-admin/categoria-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environment';

export interface CategoriaResponse {
  id: number;
  nombre: string;
  slug: string;
  tiendaId: number;
}

export interface CategoriaPage {
  content: CategoriaResponse[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface CategoriaRequest {
  nombre: string;
  slug: string;
  tiendaId?: number; // ← opcional: solo ADMIN lo envía si quiere asignar a otra tienda
}

@Injectable({
  providedIn: 'root'
})
export class CategoriaAdminService {
  private readonly BASE_URL = `${environment.apiUrl}/api/owner/categorias`;

  constructor(private http: HttpClient) {}

  // LISTAR
  listarCategorias(
    page: number = 0,
    size: number = 20,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<CategoriaPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<CategoriaPage>(`${this.BASE_URL}/admin-list`, { params })
      .pipe(catchError(this.handleError));
  }

crearCategoria(request: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(this.BASE_URL, request)
      .pipe(catchError(this.handleError));
  }

  actualizarCategoria(id: number, request: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.BASE_URL}/${id}`, request)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    let mensaje = 'Error desconocido';
    let titulo = 'Error';

    if (error.error instanceof ErrorEvent) {
      mensaje = error.error.message;
    } else {
      switch (error.status) {
        case 400:
          mensaje = error.error?.message || 'Datos inválidos';
          break;
        case 403:
          mensaje = error.error?.message || 'No tienes permiso';
          break;
        case 404:
          mensaje = 'Recurso no encontrado';
          break;
        default:
          // Aquí capturamos el mensaje que envías desde el backend
          mensaje = error.error?.message || `Error del servidor (${error.status})`;
          if (mensaje.includes('No tienes una tienda asociada')) {
            titulo = 'Falta tu tienda';
          }
          break;
      }
    }

    // Devolvemos un objeto rico para usar en SweetAlert
    return throwError(() => ({ titulo, mensaje }));
  }

  // ELIMINAR (opcional, si lo vas a usar)
  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // Helper: generar slug
  generarSlug(nombre: string): string {
    return nombre
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .substring(0, 50);
  }

 
}