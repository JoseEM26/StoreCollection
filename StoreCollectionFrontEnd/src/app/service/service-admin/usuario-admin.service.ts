// src/app/services/usuario-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import { UsuarioPage } from '../../model/admin/usuario-admin.model';

@Injectable({
  providedIn: 'root'
})
export class UsuarioAdminService {
  private apiUrl = `${environment.apiUrl}/api/admin/usuarios`;

  constructor(private http: HttpClient) {}

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
}