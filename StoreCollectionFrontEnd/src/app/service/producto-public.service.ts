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
    return this.http.get<ProductoPublic[]>(
      `${this.tiendaService.getBaseUrl()}/productos`
    );
  }

  getBySlug(slug: string): Observable<ProductoPublic> {
    return this.http.get<ProductoPublic>(
      `${this.tiendaService.getBaseUrl()}/productos/${slug}`
    );
  }
}