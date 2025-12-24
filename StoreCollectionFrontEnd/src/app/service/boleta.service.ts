// src/app/service/boleta.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment';
import { BoletaResponse } from '../model/boleta.model';

@Injectable({
  providedIn: 'root'
})
export class BoletaService {
  private apiUrl = `${environment.apiUrl}/api/public/boleta`;

  constructor(private http: HttpClient) {}

  /** Lista todas las boletas de una tienda (para el admin) */
  getBoletasPorTienda(tiendaId: number): Observable<BoletaResponse[]> {
    return this.http.get<BoletaResponse[]>(`${this.apiUrl}/tienda/${tiendaId}`);
  }

  /** Lista boletas asociadas a una session (opcional, para cliente anónimo) */
  getBoletasPorSession(sessionId: string): Observable<BoletaResponse[]> {
    return this.http.get<BoletaResponse[]>(`${this.apiUrl}/session/${sessionId}`);
  }

  /** Obtiene una boleta específica por ID */
  getBoletaPorId(id: number): Observable<BoletaResponse> {
    return this.http.get<BoletaResponse>(`${this.apiUrl}/${id}`);
  }

  /** Actualiza el estado de una boleta (PENDIENTE → ATENDIDA o CANCELADA) */
  actualizarEstado(id: number, estado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'): Observable<BoletaResponse> {
    return this.http.put<BoletaResponse>(`${this.apiUrl}/${id}/estado`, { estado });
  }
}