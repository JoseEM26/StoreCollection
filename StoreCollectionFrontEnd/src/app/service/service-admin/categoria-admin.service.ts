// src/app/service/service-admin/categoria-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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
  tiendaId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CategoriaAdminService {
  private readonly BASE_URL = `${environment.apiUrl}/api/owner/categorias`;

  constructor(private http: HttpClient) {}

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

    return this.http.get<CategoriaPage>(`${this.BASE_URL}/admin-list`, { params });
  }

  // CREAR CATEGORÍA
  crearCategoria(request: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(this.BASE_URL, request);
  }

  // Generar slug automático (opcional)
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