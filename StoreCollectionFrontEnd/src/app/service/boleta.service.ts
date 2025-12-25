// src/app/service/boleta.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environment';
import { BoletaResponse } from '../model/boleta.model';

// Interfaz para la respuesta paginada (muy común en Spring Boot Page<T>)
export interface BoletaPageResponse {
  content: BoletaResponse[];         // lista de boletas
  totalElements: number;             // total de registros
  totalPages: number;                // total de páginas
  number: number;                    // página actual (0-based)
  size: number;                      // tamaño de página
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BoletaService {
  private apiUrl = `${environment.apiUrl}/api/owner/boletas`; // Ruta base del controlador owner

  constructor(private http: HttpClient) {}

  /**
   * Obtiene boletas paginadas (para admin/owner)
   * @param page Número de página (0-based)
   * @param size Tamaño de página
   * @param sort Ordenamiento (ej: 'fecha,desc')
   * @param estado Filtro opcional por estado
   * @param tiendaId Filtro opcional por tienda (multi-tenant)
   */
  getBoletasPaginadas(
    page: number = 0,
    size: number = 20,
    sort: string = 'fecha,desc',
    estado?: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA',
    tiendaId?: number
  ): Observable<BoletaPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (estado) {
      params = params.set('estado', estado);
    }

    if (tiendaId !== undefined && tiendaId !== null) {
      params = params.set('tiendaId', tiendaId.toString());
    }

    // Depuración (puedes quitar en producción)
    console.log('[BoletaService] Solicitando boletas con params:', params.toString());

    return this.http.get<BoletaPageResponse>(`${this.apiUrl}/admin-list`, { params }).pipe(
      map(response => {
        // Opcional: normalizar datos si es necesario
        return response;
      }),
      catchError(error => {
        console.error('[BoletaService] Error al obtener boletas paginadas:', error);
        return throwError(() => new Error('No se pudieron cargar las boletas. Intenta nuevamente más tarde.'));
      })
    );
  }
public actualizarEstado(id: number, estado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'): Observable<BoletaResponse> {
  console.log(`[BoletaService] Actualizando boleta #${id} → estado: ${estado}`);

  return this.http.put<BoletaResponse>(`${this.apiUrl}/${id}/estado`, { estado }).pipe(
    catchError(error => {
      console.error(`[BoletaService] Error actualizando estado de boleta #${id}:`, error);
      return throwError(() => new Error(`No se pudo cambiar el estado a ${estado}`));
    })
  );
}


  /**
   * Obtiene el detalle de una boleta específica por su ID
   */
  getBoletaPorId(id: number): Observable<BoletaResponse> {
    return this.http.get<BoletaResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error(`[BoletaService] Error al obtener boleta #${id}:`, error);
        return throwError(() => new Error('No se pudo cargar el detalle de la boleta'));
      })
    );
  }

  

 // Métodos de conveniencia (pueden seguir siendo públicos)
marcarComoAtendida(id: number): Observable<BoletaResponse> {
  return this.actualizarEstado(id, 'ATENDIDA');
}

cancelarBoleta(id: number): Observable<BoletaResponse> {
  return this.actualizarEstado(id, 'CANCELADA');
}
}