import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import {
  ProductoPage,
  ProductoResponse,
  ProductoRequest
} from '../../model/admin/producto-admin.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {

  // Ruta base correcta (sin "/admin-list")
  private baseUrl = `${environment.apiUrl}/api/owner/productos`;

  constructor(private http: HttpClient) {}

  // ======================== LISTAR PRODUCTOS ========================
  listarProductos(
    page: number = 0,
    size: number = 20,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<ProductoPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    // Ruta correcta: solo /api/owner/productos
    return this.http.get<ProductoPage>(this.baseUrl, { params });
  }

  // ======================== CREAR PRODUCTO ========================
  crearProducto(request: FormData): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(this.baseUrl, request);
  }

  // ======================== OBTENER PARA EDICIÓN ========================
  obtenerParaEdicion(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(`${this.baseUrl}/${id}`);
  }

  // ======================== ACTUALIZAR PRODUCTO ========================
  actualizarProducto(id: number, request: FormData): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.baseUrl}/${id}`, request);
  }

  // ======================== TOGGLE ACTIVO ========================
  toggleActivo(id: number): Observable<ProductoResponse> {
    return this.http.patch<ProductoResponse>(`${this.baseUrl}/${id}/toggle-activo`, null);
  }

  // ======================== ELIMINAR PRODUCTO (opcional, si lo usas) ========================
  eliminarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}