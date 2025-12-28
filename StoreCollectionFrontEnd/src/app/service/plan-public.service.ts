// src/app/service/service-public/plan-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment';
import { PlanResponse } from '../model/admin/plan-admin.model';
import { DropTownStandar } from './droptown.service';

@Injectable({
  providedIn: 'root'
})
export class PlanPublicService {
  private publicUrl = `${environment.apiUrl}/api/public/planes`;
  private dropdownUrl = `${environment.apiUrl}/api/planes/dropdown`;

  constructor(private http: HttpClient) {}

  // Lista completa sin paginación (ideal para página de precios)
  obtenerPlanesPublicos(): Observable<PlanResponse[]> {
    return this.http.get<PlanResponse[]>(this.publicUrl);
  }

  // Lista paginada (si necesitas filtros o más control)
  obtenerPlanesPaginados(
    page: number = 0,
    size: number = 10,
    sort: string[] = ['orden,asc']
  ): Observable<any> {  // Puedes crear un PlanPage si lo deseas
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    sort.forEach(s => params = params.append('sort', s));

    return this.http.get<any>(`${this.publicUrl}/paginado`, { params });
  }

  // Para dropdown de selección rápida (ej: al crear tienda o cambiar plan)
  obtenerDropdown(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(this.dropdownUrl);
  }
}