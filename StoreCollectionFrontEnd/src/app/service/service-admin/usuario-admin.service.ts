// src/app/services/usuario-admin.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import { UsuarioPage, UsuarioRequest, UsuarioResponse } from '../../model/admin/usuario-admin.model';

@Injectable({
  providedIn: 'root'
})
export class UsuarioAdminService {
  private apiUrl = `${environment.apiUrl}/api/admin/usuarios`;

  constructor(private http: HttpClient) {}

  // Listar con paginación y búsqueda opcional
  listarUsuarios(
    page: number = 0,
    size: number = 10,
    search?: string
  ): Observable<UsuarioPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<UsuarioPage>(this.apiUrl, { params });
  }

  // Obtener un usuario por ID (para editar)
  obtenerPorId(id: number): Observable<UsuarioResponse> {
    return this.http.get<UsuarioResponse>(`${this.apiUrl}/${id}`);
  }

  // Crear nuevo usuario
  crear(usuario: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.apiUrl, usuario);
  }

  // Actualizar usuario existente
  actualizar(id: number, usuario: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.apiUrl}/${id}`, usuario);
  }

  // Toggle: activar/desactivar usuario
  toggleActivo(id: number): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.apiUrl}/${id}/toggle-activo`, null);
  }
}