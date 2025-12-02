// src/app/services/producto-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable } from 'rxjs';
import { Producto, Page } from '../model';

@Injectable({ providedIn: 'root' })
export class ProductoPublicService {

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  /** Todos los productos de la tienda actual */
  getAll(): Observable<Producto[]> {
    const url = `${this.tiendaService.getBaseUrl()}/productos`;
    return this.http.get<Producto[]>(url);
  }

  /** Producto por slug (para página de detalle) */
  getBySlug(productoSlug: string): Observable<Producto> {
    const url = `${this.tiendaService.getBaseUrl()}/productos/${productoSlug}`;
    return this.http.get<Producto>(url);
  }

  /** Opcional: si más adelante quieres paginación */
  getPaged(page: number = 0, size: number = 20): Observable<Page<Producto>> {
    const url = `${this.tiendaService.getBaseUrl()}/productos/paged?page=${page}&size=${size}`;
    return this.http.get<Page<Producto>>(url);
  }
}