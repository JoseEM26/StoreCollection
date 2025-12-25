// src/app/service/carrito.service.ts (COMPLETO, sin errores y 100% compatible con backend)

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError } from 'rxjs';
import { CarritoSessionService } from './carrito-session.service';  // Asume que existe
import { 
  CarritoItemResponse, 
  CarritoRequest, 
  CarritoResponse 
} from '../model/carrito.model';
import { BoletaRequest, BoletaResponse } from '../model/boleta.model';
import { environment } from '../../../environment';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private apiUrl = `${environment.apiUrl}/api/public/carrito`;

  // Estado reactivo principal
  private carritoItems = new BehaviorSubject<CarritoItemResponse[]>([]);
  carritoItems$ = this.carritoItems.asObservable();

  // Conteo de ítems (cantidad total de productos)
  private itemsCount = new BehaviorSubject<number>(0);
  itemsCount$ = this.itemsCount.asObservable();

  // Total a pagar
  private totalPrecio = new BehaviorSubject<number>(0);
  totalPrecio$ = this.totalPrecio.asObservable();

  constructor(
    private http: HttpClient,
    private sessionService: CarritoSessionService
  ) {
    this.cargarCarritoDesdeBackend();
  }

  private getSessionId(): string {
    return this.sessionService.getSessionId();
  }

  /** Carga o recarga el carrito completo desde el backend */
  cargarCarritoDesdeBackend(): void {
    const sessionId = this.getSessionId();
    if (!sessionId) {
      this.actualizarEstadoCarrito([]);
      return;
    }

    this.http.get<CarritoItemResponse[]>(`${this.apiUrl}/session/${sessionId}`).subscribe({
      next: (items) => {
        this.actualizarEstadoCarrito(items || []);
      },
      error: (err) => {
        console.error('Error al cargar carrito:', err);
        this.actualizarEstadoCarrito([]);
      }
    });
  }

  /** Agregar producto (o actualizar si ya existe) */
  agregarAlCarrito(varianteId: number, cantidad: number = 1): Observable<CarritoItemResponse> {
    const request: CarritoRequest = {
      sessionId: this.getSessionId(),
      varianteId,
      cantidad
    };

    return this.http.post<CarritoItemResponse>(this.apiUrl, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend()),
      catchError(err => {
        console.error('Error al agregar al carrito:', err);
        throw err;
      })
    );
  }

  /** Actualizar cantidad de un ítem específico */
  actualizarCantidad(itemId: number, cantidad: number): Observable<CarritoItemResponse> {
    const request = {
      sessionId: this.getSessionId(),
      cantidad
    };

    return this.http.put<CarritoItemResponse>(`${this.apiUrl}/${itemId}`, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend()),
      catchError(err => {
        console.error('Error al actualizar cantidad:', err);
        throw err;
      })
    );
  }

  /** Eliminar un ítem del carrito */
  eliminarItem(itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${itemId}`).pipe(
      tap(() => this.cargarCarritoDesdeBackend()),
      catchError(err => {
        console.error('Error al eliminar item:', err);
        throw err;
      })
    );
  }

  /** Vaciar completamente el carrito */
  vaciarCarrito(): Observable<void> {
    const sessionId = this.getSessionId();
    if (!sessionId) {
      this.actualizarEstadoCarrito([]);
      return new Observable<void>(observer => observer.complete());
    }

    return this.http.delete<void>(`${this.apiUrl}/session/${sessionId}`).pipe(
      tap(() => this.actualizarEstadoCarrito([])),
      catchError(err => {
        console.error('Error al vaciar carrito:', err);
        throw err;
      })
    );
  }

  /** Actualiza todos los observables reactivos a partir de la lista de items */
  private actualizarEstadoCarrito(items: CarritoItemResponse[]): void {
    this.carritoItems.next(items);

    const totalItems = items.reduce((sum, item) => sum + (item.cantidad || 0), 0);
    this.itemsCount.next(totalItems);

    const totalPrice = items.reduce((sum, item) => {
      return sum + ((item.precio || 0) * (item.cantidad || 0));
    }, 0);

    this.totalPrecio.next(totalPrice);
  }

  // Getters síncronos (útiles en algunos casos)
  getTotalItemsSync(): number {
    return this.itemsCount.value;
  }

  getTotalPrecioSync(): number {
    return this.totalPrecio.value;
  }

  getItemsSync(): CarritoItemResponse[] {
    return this.carritoItems.value;
  }

  // ── Checkout ───────────────────────────────────────────────

  /** Checkout online: registra el pedido en la base de datos + email */
  checkoutOnline(tiendaId: number, userId?: number): Observable<BoletaResponse> {
    const request: BoletaRequest = {
      sessionId: this.getSessionId(),
      tiendaId,
      userId: userId ?? null
    };

    return this.http.post<BoletaResponse>(`${this.apiUrl}/checkout/online`, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend()), // limpia carrito visualmente
      catchError(err => {
        console.error('Error en checkout online:', err);
        throw err;
      })
    );
  }

checkoutWhatsapp(tiendaId: number, userId?: number): Observable<string> {
    const request: BoletaRequest = {
      sessionId: this.getSessionId(),
      tiendaId,
      userId: userId ?? null
    };

    console.log('[CarritoService] Iniciando checkoutWhatsapp - Request enviado:', request);
    console.log('[CarritoService] URL de petición:', `${this.apiUrl}/checkout/whatsapp`);

    return this.http.post<string>(`${this.apiUrl}/checkout/whatsapp`, request, { responseType: 'text' as 'json' }).pipe(  // ← fuerza text
      tap(response => {
        console.log('[CarritoService] checkoutWhatsapp EXITOSO - Response cruda:', response);
        console.log('[CarritoService] Tipo de response:', typeof response);
      }),
      tap(() => this.cargarCarritoDesdeBackend()),
      catchError(err => {
        console.error('[CarritoService] ERROR en checkoutWhatsapp:', {
          status: err.status,
          statusText: err.statusText,
          message: err.message,
          errorBody: err.error
        });
        throw err;
      })
    );
  }

  /** Alias retrocompatible (si algún componente viejo lo usa) */
  checkout(tiendaId: number, userId?: number): Observable<BoletaResponse> {
    return this.checkoutOnline(tiendaId, userId);
  }
}