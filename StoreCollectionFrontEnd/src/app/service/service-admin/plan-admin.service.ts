// src/app/service/service-admin/plan-admin.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlanPage, PlanRequest, PlanResponse } from '../../model/admin/plan-admin.model';
import { environment } from '../../../../environment';

@Injectable({
  providedIn: 'root'
})
export class PlanAdminService {
  private adminUrl = `${environment.apiUrl}/api/admin/planes`;
  private publicUrl = `${environment.apiUrl}/api/public/planes`;

  constructor(private http: HttpClient) {}

  // Lista paginada para admin (incluye campo 'activo')
  listar(page: number = 0, size: number = 10, search?: string): Observable<PlanPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PlanPage>(this.publicUrl, { params });
  }

  obtenerPorId(id: number): Observable<PlanResponse> {
    return this.http.get<PlanResponse>(`${this.adminUrl}/${id}`);
  }

  crear(plan: PlanRequest): Observable<PlanResponse> {
    return this.http.post<PlanResponse>(this.adminUrl, plan);
  }

  actualizar(id: number, plan: PlanRequest): Observable<PlanResponse> {
    return this.http.put<PlanResponse>(`${this.adminUrl}/${id}`, plan);
  }

  toggleActivo(id: number): Observable<PlanResponse> {
    return this.http.patch<PlanResponse>(`${this.adminUrl}/${id}/toggle-activo`, {});
  }
}