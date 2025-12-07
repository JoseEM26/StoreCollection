// src/app/services/categoria-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import { CategoriaPage } from '../../model/admin/categoria-admin.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriaAdminService {
  private apiUrl = `${environment.apiUrl}/api/owner/categorias/admin-list`;

  constructor(private http: HttpClient) {}

  listarCategorias(
    page: number = 0,
    size: number = 20,
    sort: string = 'nombre,asc',
    search?: string
  ): Observable<CategoriaPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<CategoriaPage>(this.apiUrl, { params });
  }
}