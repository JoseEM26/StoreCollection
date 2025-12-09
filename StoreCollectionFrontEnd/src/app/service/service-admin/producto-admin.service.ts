// src/app/services/producto-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';

// Modelos de respuesta
import { ProductoPage, ProductoResponse } from '../../model/admin/producto-admin.model';

export interface ProductoCreateRequest {
  id?: number;
  tiendaId?: number;        // Solo ADMIN lo envía
  nombre: string;
  slug: string;
  categoriaId: number;
  variantes: VarianteCreateRequest[];
}

export interface VarianteCreateRequest {
  id?: number;
  sku: string;
  precio: number;
  stock?: number;
  imagenUrl?: string;
  activo?: boolean;
  atributoValorIds?: number[];
}

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {

  private readonly baseUrl = `${environment.apiUrl}/api/owner`;

  constructor(private http: HttpClient) {}

  // === LISTAR PRODUCTOS (paginado) ===
  listarProductos(
    page = 0,
    size = 20,
    sort = 'nombre,asc',
    search?: string
  ): Observable<ProductoPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<ProductoPage>(`${this.baseUrl}/productos/admin-list`, { params });
  }

  // === CREAR PRODUCTO COMPLETO ===
  crearProducto(request: ProductoCreateRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(`${this.baseUrl}/productos/completo`, request);
  }

  // === ACTUALIZAR PRODUCTO COMPLETO ===
  actualizarProducto(id: number, request: ProductoCreateRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.baseUrl}/productos/completo/${id}`, request);
  }

  // === OBTENER UN PRODUCTO POR ID (para editar) ===
  obtenerProducto(id: number): Observable<ProductoResponse> {
    return this.http.get<ProductoResponse>(`${this.baseUrl}/productos/${id}`);
  }

  // === DROPDOWNS NECESARIOS PARA EL FORMULARIO ===

  // Categorías (filtradas por rol)
  obtenerCategorias(): Observable<CategoriaDropdown[]> {
    return this.http.get<CategoriaDropdown[]>(`${this.baseUrl}/dropdown/categorias`);
  }

  // Atributos + valores (Color, Talla, etc.)
  obtenerAtributos(): Observable<AtributoConValores[]> {
    return this.http.get<AtributoConValores[]>(`${this.baseUrl}/dropdown/atributos`);
  }

  // Tiendas (solo para ADMIN)
  obtenerTiendas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/tiendas/dropdown`);
  }
}