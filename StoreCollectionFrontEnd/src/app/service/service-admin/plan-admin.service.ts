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

  constructor(private http: HttpClient) {}

  // Lista paginada para admin (todos los planes, activos e inactivos)
  listar(
    page: number = 0,
    size: number = 10,
    sort: string[] = ['id,desc']  // Valor por defecto del backend
  ): Observable<PlanPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    sort.forEach(s => params = params.append('sort', s));

    return this.http.get<PlanPage>(this.adminUrl, { params });
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

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }
}