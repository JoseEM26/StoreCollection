// src/app/services/producto-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable } from 'rxjs';
import { ProductoPublic } from '../model/index.dto';

@Injectable({ providedIn: 'root' })
export class ProductoPublicService {
  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  getAll(): Observable<ProductoPublic[]> {
    const base = this.tiendaService.getBaseUrl(); // ej: http://localhost:8080/api/public/tiendas/zapatik
    return this.http.get<ProductoPublic[]>(`${base}/productos`);
  }

  getBySlug(slug: string): Observable<ProductoPublic> {
    const base = this.tiendaService.getBaseUrl();
    return this.http.get<ProductoPublic>(`${base}/productos/${slug}`);
  }
}