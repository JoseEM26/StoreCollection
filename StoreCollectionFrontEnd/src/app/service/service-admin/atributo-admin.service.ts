// src/app/service/admin/atributo-admin.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';
import { PageResponse } from './dashboard.service';

export interface AtributoListItem {
  id: number;
  nombre: string;
  tiendaId: number;
  tiendaNombre: string;
  valor?: string | null; // Mantenido por compatibilidad con otros usos
}

export interface AtributoCreateRequest {
  nombre: string;
  tiendaId?: number; // Solo para admin
}

export interface AtributoUpdateRequest {
  nombre: string;
  tiendaId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AtributoAdminService {

  private ownerBaseUrl = '/api/owner/atributos';
  private adminBaseUrl = '/api/admin/atributos';

  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  private getBaseUrl(): string {
    return this.auth.isAdmin() ? this.adminBaseUrl : this.ownerBaseUrl;
  }

  // === LISTAR (Owner: solo suyos | Admin: todos con paginación y filtro opcional) ===
  listar(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'nombre',
    sortDir: 'asc' | 'desc' = 'asc',
    tiendaId?: number
  ): Observable<PageResponse<AtributoListItem>> {

    const url = this.auth.isAdmin() ? this.adminBaseUrl : this.ownerBaseUrl;

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (tiendaId !== undefined && tiendaId !== null && this.auth.isAdmin()) {
      params = params.set('tiendaId', tiendaId.toString());
    }

    return this.http.get<PageResponse<AtributoListItem>>(url, { params });
  }

  // === LISTAR SIMPLES (para owner, sin paginación - útil para dropdowns o listas rápidas) ===
  listarSimples(): Observable<AtributoListItem[]> {
    return this.http.get<AtributoListItem[]>(this.ownerBaseUrl);
  }

  // === OBTENER UNO POR ID ===
  obtenerPorId(id: number): Observable<AtributoListItem> {
    const url = `${this.getBaseUrl()}/${id}`;
    return this.http.get<AtributoListItem>(url);
  }

  // === CREAR ===
  crear(request: AtributoCreateRequest): Observable<AtributoListItem> {
    const url = this.getBaseUrl();
    return this.http.post<AtributoListItem>(url, request);
  }

  // === ACTUALIZAR ===
  actualizar(id: number, request: AtributoUpdateRequest): Observable<AtributoListItem> {
    const url = `${this.getBaseUrl()}/${id}`;
    return this.http.put<AtributoListItem>(url, request);
  }

  // === ELIMINAR ===
  eliminar(id: number): Observable<void> {
    const url = `${this.getBaseUrl()}/${id}`;
    return this.http.delete<void>(url);
  }

  // === MÉTODO ESPECIAL PARA ADMIN: Crear con tienda específica ===
  crearComoAdmin(request: AtributoCreateRequest): Observable<AtributoListItem> {
    if (!this.auth.isAdmin()) {
      throw new Error('Solo administradores pueden usar este método');
    }
    return this.http.post<AtributoListItem>(this.adminBaseUrl, request);
  }
}