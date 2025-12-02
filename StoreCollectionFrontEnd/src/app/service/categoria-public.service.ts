// src/app/services/categoria-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable } from 'rxjs';
import { Categoria } from '../model';

@Injectable({ providedIn: 'root' })
export class CategoriaPublicService {

  constructor(
    private http: HttpClient,
    private tiendaService: TiendaService
  ) {}

  /** Lista todas las categorías de la tienda actual (por slug) */
  getAll(): Observable<Categoria[]> {
    const url = `${this.tiendaService.getBaseUrl()}/categorias`;
    return this.http.get<Categoria[]>(url);
  }
}