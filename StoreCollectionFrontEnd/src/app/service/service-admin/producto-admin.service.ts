// src/app/services/producto-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';
import { ProductoPage } from '../../model/admin/producto-admin.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoAdminService {
  private apiUrl = `${environment.apiUrl}/api/owner/productos/admin-list`;

  constructor(private http: HttpClient) {}

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

    return this.http.get<ProductoPage>(this.apiUrl, { params });
  }
}