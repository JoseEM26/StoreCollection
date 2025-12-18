import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import {
  CategoriaPage,
  CategoriaResponse,
  CategoriaRequest
} from '../../model/admin/categoria-admin.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriaAdminService {
  private baseUrl = `${environment.apiUrl}/api/owner/categorias`;

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

    return this.http.get<CategoriaPage>(this.baseUrl+"/admin-list", { params });
  }

  // === CREAR NUEVA CATEGORÍA ===
  crearCategoria(request: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(this.baseUrl, request);
  }

  // === OBTENER UNA CATEGORÍA PARA EDICIÓN ===
  obtenerParaEdicion(id: number): Observable<CategoriaResponse> {
    return this.http.get<CategoriaResponse>(`${this.baseUrl}/${id}`);
  }

  // === ACTUALIZAR CATEGORÍA ===
  actualizarCategoria(id: number, request: CategoriaRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.baseUrl}/${id}`, request);
  }

  // === TOGGLE ACTIVO / INACTIVO (solo ADMIN) ===
  toggleActivo(id: number): Observable<CategoriaResponse> {
    return this.http.patch<CategoriaResponse>(`${this.baseUrl}/${id}/toggle-activo`, null);
  }
}