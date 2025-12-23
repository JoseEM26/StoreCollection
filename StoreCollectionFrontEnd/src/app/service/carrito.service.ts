// src/app/service/carrito.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { CarritoSessionService } from './carrito-session.service';
import { CarritoItemResponse, CarritoRequest, CarritoResponse } from '../model/carrito.model';
import { BoletaRequest, BoletaResponse } from '../model/boleta.model';
import { environment } from '../../../environment';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private apiUrl = `${environment.apiUrl}/api/public/carrito`;

  // Estado reactivo del carrito
  private carritoItems = new BehaviorSubject<CarritoItemResponse[]>([]);
  carritoItems$ = this.carritoItems.asObservable();

  private itemsCount = new BehaviorSubject<number>(0);
  itemsCount$ = this.itemsCount.asObservable();

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

  /** Carga el carrito desde el backend */
  cargarCarritoDesdeBackend(): void {
    const sessionId = this.getSessionId();
    if (!sessionId) {
      this.actualizarEstadoCarrito([]);
      return;
    }

    this.http.get<CarritoResponse>(`${this.apiUrl}/session/${sessionId}`).subscribe({
      next: (items) => {
        this.actualizarEstadoCarrito(items);
      },
      error: (err) => {
        console.error('Error cargando carrito:', err);
        this.actualizarEstadoCarrito([]);
      }
    });
  }

  /** Agregar o actualizar (si ya existe, el backend lo maneja) */
  agregarAlCarrito(varianteId: number, cantidad: number = 1): Observable<CarritoItemResponse> {
    const request: CarritoRequest = {
      sessionId: this.getSessionId(),
      varianteId,
      cantidad
    };

    return this.http.post<CarritoItemResponse>(this.apiUrl, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend())
    );
  }

  /** Actualizar cantidad de un item existente */
  actualizarCantidad(itemId: number, cantidad: number): Observable<CarritoItemResponse> {
    const request: Partial<CarritoRequest> = {
      sessionId: this.getSessionId(),
      cantidad
    };

    return this.http.put<CarritoItemResponse>(`${this.apiUrl}/${itemId}`, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend())
    );
  }

  /** Eliminar un item */
  eliminarItem(itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${itemId}`).pipe(
      tap(() => this.cargarCarritoDesdeBackend())
    );
  }

  /** Vaciar todo el carrito */
  vaciarCarrito(): Observable<void> {
    const sessionId = this.getSessionId();
    if (!sessionId) {
      this.actualizarEstadoCarrito([]);
      return new Observable<void>();
    }

    return this.http.delete<void>(`${this.apiUrl}/session/${sessionId}`).pipe(
      tap(() => this.actualizarEstadoCarrito([]))
    );
  }

  /** Procesar compra (checkout) */
  checkout(tiendaId: number, userId?: number): Observable<BoletaResponse> {
    const request: BoletaRequest = {
      sessionId: this.getSessionId(),
      tiendaId,
      userId
    };

    return this.http.post<BoletaResponse>(`${this.apiUrl}/checkout`, request).pipe(
      tap(() => {
        // El backend ya limpia el carrito, pero recargamos por seguridad
        this.cargarCarritoDesdeBackend();
      })
    );
  }

  /** Actualiza todos los observables */
  private actualizarEstadoCarrito(items: CarritoItemResponse[]): void {
    this.carritoItems.next(items);

    const totalItems = items.reduce((sum, item) => sum + item.cantidad, 0);
    this.itemsCount.next(totalItems);

    const totalPrice = items.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
    this.totalPrecio.next(totalPrice);
  }

  // Getters sincronos
  getTotalItems(): number {
    return this.itemsCount.value;
  }

  getTotalPrecio(): number {
    return this.totalPrecio.value;
  }

  getItems(): CarritoItemResponse[] {
    return this.carritoItems.value;
  }
}