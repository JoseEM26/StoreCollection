// producto-admin.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import {
  ProductoAdminListPage,
  ProductoResponse,
  ProductoRequest
} from '../../model/admin/producto-admin.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {

  private baseUrl = `${environment.apiUrl}/api/owner/productos`;

  constructor(private http: HttpClient) {}

  listarProductos(
    page: number = 0,
    size: number = 20,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<ProductoAdminListPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<ProductoAdminListPage>(`${this.baseUrl}/admin-list`, { params });
  }

  crearProducto(request: FormData): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.baseUrl, request);
  }

  obtenerParaEdicion(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(`${this.baseUrl}/${id}`);
  }

  actualizarProducto(id: number, request: FormData): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.baseUrl}/${id}`, request);
  }

  toggleActivo(id: number): Observable<ProductoResponse> {
    return this.http.patch<ProductoResponse>(`${this.baseUrl}/${id}/toggle-activo`, null);
  }

  eliminarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}