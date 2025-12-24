import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BoletaPageResponse, BoletaResponse } from '../model/boleta.model';
import { environment } from '../../../environment';

@Injectable({
  providedIn: 'root'
})
export class BoletaService {
  private apiUrl = `${environment.apiUrl}/api/owner/boletas`; // Ruta real del controlador

  constructor(private http: HttpClient) {}

  /**
   * Obtiene boletas paginadas (para admin/owner)
   * @param estado Opcional: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA' (mayúsculas)
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

    // Solo agregar estado si existe y es válido
    if (estado && ['PENDIENTE', 'ATENDIDA', 'CANCELADA'].includes(estado)) {
      params = params.set('estado', estado);
    }

    if (tiendaId !== undefined && tiendaId !== null) {
      params = params.set('tiendaId', tiendaId.toString());
    }

    console.log('Parámetros enviados al backend:', params.toString()); // ← Para depuración

    return this.http.get<BoletaPageResponse>(`${this.apiUrl}/admin-list`, { params }).pipe(
      catchError(error => {
        console.error('Error al obtener boletas paginadas:', error);
        return throwError(() => new Error('No se pudieron cargar las boletas. Intenta nuevamente.'));
      })
    );
  }

  /**
   * Obtiene una boleta específica por ID
   */
  getBoletaPorId(id: number): Observable<BoletaResponse> {
    return this.http.get<BoletaResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error(`Error al obtener boleta ${id}:`, error);
        return throwError(() => new Error('No se pudo cargar el detalle de la boleta'));
      })
    );
  }

  /**
   * Actualiza el estado de una boleta
   */
  actualizarEstado(
    id: number,
    estado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'
  ): Observable<BoletaResponse> {
    console.log(`Actualizando boleta ${id} a estado: ${estado}`); // Depuración

    return this.http.put<BoletaResponse>(`${this.apiUrl}/${id}/estado`, { estado }).pipe(
      catchError(error => {
        console.error(`Error al actualizar estado de boleta ${id}:`, error);
        return throwError(() => new Error(`No se pudo actualizar el estado a ${estado}`));
      })
    );
  }

  // Conveniencia
  marcarComoAtendida(id: number): Observable<BoletaResponse> {
    return this.actualizarEstado(id, 'ATENDIDA');
  }

  cancelarBoleta(id: number): Observable<BoletaResponse> {
    return this.actualizarEstado(id, 'CANCELADA');
  }
}