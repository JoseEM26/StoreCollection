// src/app/services/boleta.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environment';
import { BoletaPageResponse, BoletaResponse } from '../model/boleta.model';

@Injectable({
  providedIn: 'root'
})
export class BoletaService {
  private apiUrl = `${environment.apiUrl}/api/owner/boletas`;

  constructor(private http: HttpClient) {}

  // =============================================
  // Listado paginado (admin / owner)
  // =============================================
  getBoletasPaginadas(
    page: number = 0,
    size: number = 20,
    sort: string = 'fecha,desc',
    estado?: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'
  ): Observable<BoletaPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (estado) {
      params = params.set('estado', estado);
    }

    return this.http.get<BoletaPageResponse>(`${this.apiUrl}/admin-list`, { params }).pipe(
      catchError(error => {
        console.error('[BoletaService] Error al cargar lista de boletas:', error);
        return throwError(() => new Error('Error al cargar las boletas. Inténtalo de nuevo.'));
      })
    );
  }

  // =============================================
  // Detalle de boleta
  // =============================================
  getBoletaPorId(id: number): Observable<BoletaResponse> {
    return this.http.get<BoletaResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(error => {
        console.error(`[BoletaService] Error al obtener boleta #${id}:`, error);
        return throwError(() => new Error('No se pudo cargar el detalle de la boleta.'));
      })
    );
  }

  // =============================================
  // Cambio de estado
  // =============================================
  private actualizarEstado(id: number, estado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA'): Observable<BoletaResponse> {
    const body = { estado };
    console.log(`[BoletaService] Cambiando estado de boleta #${id} → ${estado}`);

    return this.http.put<BoletaResponse>(`${this.apiUrl}/${id}/estado`, body).pipe(
      catchError(error => {
        console.error(`[BoletaService] Error al actualizar estado de boleta #${id}:`, error);
        let mensaje = 'No se pudo actualizar el estado de la boleta.';
        if (error.status === 400) {
          mensaje = error.error?.message || 'Estado inválido.';
        } else if (error.status === 403) {
          mensaje = 'No tienes permiso para realizar esta acción.';
        } else if (error.status === 404) {
          mensaje = 'Boleta no encontrada.';
        }
        return throwError(() => new Error(mensaje));
      })
    );
  }

  public marcarComoAtendida(id: number): Observable<BoletaResponse> {
    return this.actualizarEstado(id, 'ATENDIDA');
  }

  public cancelarBoleta(id: number): Observable<BoletaResponse> {
    return this.actualizarEstado(id, 'CANCELADA');
  }

  public volverAPendiente(id: number): Observable<BoletaResponse> {
    return this.actualizarEstado(id, 'PENDIENTE');
  }

  // =============================================
  // ENVÍO DE CONFIRMACIÓN POR WHATSAPP AL CLIENTE
  // =============================================
  generarWhatsappConfirmacionCliente(id: number): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/${id}/whatsapp-confirmacion`, { responseType: 'text' as 'json' }).pipe(
      map(url => url.trim()),  // Aseguramos que no haya espacios
      catchError(error => {
        console.error(`[BoletaService] Error generando WhatsApp para boleta #${id}:`, error);
        let mensaje = 'No se pudo generar el mensaje de WhatsApp.';
        if (error.status === 400) {
          mensaje = error.error || 'El cliente no tiene teléfono registrado o es inválido.';
        } else if (error.status === 403) {
          mensaje = 'No tienes permiso para esta acción.';
        } else if (error.status === 404) {
          mensaje = 'Boleta no encontrada.';
        }
        return throwError(() => new Error(mensaje));
      })
    );
  }

  // =============================================
  // DESCARGA DE FACTURA PDF
  // =============================================
  descargarFacturaPdf(id: number): Observable<Blob> {
    console.log(`[BoletaService] Solicitando descarga de factura PDF para boleta #${id}`);

    const headers = new HttpHeaders({
      'Accept': 'application/pdf'
    });

    return this.http.get(`${this.apiUrl}/${id}/factura-pdf`, {
      responseType: 'blob',
      headers,
      observe: 'response'
    }).pipe(
      map(response => {
        if (response.status === 404) {
          throw new Error('La factura aún no está disponible. La boleta debe estar en estado ATENDIDA.');
        }
        if (!response.body || response.body.size === 0) {
          throw new Error('El PDF está vacío o no se generó correctamente.');
        }
        return response.body as Blob;
      }),
      catchError(error => {
        let mensaje = 'Error al descargar la factura.';
        if (error.status === 404) {
          mensaje = 'Factura no disponible: la boleta debe estar ATENDIDA.';
        } else if (error.status === 403) {
          mensaje = 'No tienes permiso para descargar esta factura.';
        } else if (error.status >= 500) {
          mensaje = 'Error en el servidor al generar la factura.';
        } else if (error.message) {
          mensaje = error.message;
        }
        console.error('[BoletaService] Error descarga PDF:', error);
        return throwError(() => new Error(mensaje));
      })
    );
  }
}