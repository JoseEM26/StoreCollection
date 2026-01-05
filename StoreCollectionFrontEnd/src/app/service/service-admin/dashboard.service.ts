// src/app/services/dashboard.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environment';

// ========================
// INTERFACES / TIPOS DTOs
// ========================

export interface PlanUsageDto {
  planNombre: string;
  precioMensual: string ; // o number según tu serialización

  intervaloBilling: string | null;
  fechaProximaRenovacion: string | null; // ISO date string
  diasRestantesRenovacion: number;
  proximoVencimientoCerca: boolean;

  maxProductos: number;
  maxVariantes: number;
  esTrial: boolean;
  diasTrial: number;
  fechaInicioTrial: string | null;
  fechaFinTrial: string | null;

  productosActuales: number;
  variantesActuales: number;

  porcentajeProductos: number;
  porcentajeVariantes: number;
  porcentajeTiempoTrial: number;
}
// DTO para lista de tiendas en dashboard
export interface TiendaDashboardDto {
  id: number;
  nombre: string;
  slug: string;
  activo: boolean;
  planNombre: string;
  precioMensual: string;
  createdAt: string; // ISO date
}

// DTO para lista de boletas en dashboard
export enum EstadoBoleta {
  PENDIENTE = 'PENDIENTE',
  PAGADA = 'PAGADA',
  COMPLETADA = 'COMPLETADA',
  CANCELADA = 'CANCELADA',
  REEMBOLSADA = 'REEMBOLSADA'
}

export interface BoletaDashboardDto {
  id: number;
  compradorNombre: string;
  compradorEmail: string;
  total: string; // BigDecimal
  estado: EstadoBoleta;
  fecha: string; // ISO date
  tiendaNombre: string;
}

// Respuesta paginada genérica (para tiendas y boletas)
export interface PageResponse<T> {
  content: T[];
  pageable: {
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort: { sorted: boolean; unsorted: boolean; empty: boolean };
  numberOfElements: number;
  empty: boolean;
}

// Overview general (varía según rol)
export interface DashboardOverview {
  totalTiendas: number;
  totalPlanesActivos: number;
  totalBoletas: number;
  revenueTotal: string; // BigDecimal
  rol: 'ADMIN' | 'OWNER';
}

// Revenue agrupado por estado de boleta
export type RevenuePorEstado = Record<EstadoBoleta | string, string>;

// ========================
// SERVICIO
// ========================

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private baseUrl = `${environment.apiUrl}/api/owner/dashboard`;

  constructor(private http: HttpClient) { }

  // Uso del plan (solo OWNER)
  getPlanUsage(): Observable<PlanUsageDto> {
    return this.http.get<PlanUsageDto>(`${this.baseUrl}/usage`);
  }

  // Overview general (ADMIN y OWNER)
  getOverview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${this.baseUrl}/overview`);
  }

  // Lista de tiendas (paginada para ADMIN, completa para OWNER)
  getTiendas(page: number = 0, size: number = 10): Observable<PageResponse<TiendaDashboardDto> | TiendaDashboardDto[]> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<TiendaDashboardDto> | TiendaDashboardDto[]>(`${this.baseUrl}/tiendas`, { params });
  }

  // Lista de boletas (paginada)
  getBoletas(page: number = 0, size: number = 10): Observable<PageResponse<BoletaDashboardDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<BoletaDashboardDto>>(`${this.baseUrl}/boletas`, { params });
  }

  // Revenue agrupado por estado
  getRevenuePorEstado(): Observable<RevenuePorEstado> {
    return this.http.get<RevenuePorEstado>(`${this.baseUrl}/revenue-por-estado`);
  }
}