import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';
import { CarritoSessionService } from './carrito-session.service';
import { 
  CarritoItemResponse, 
  CarritoRequest, 
  CarritoResponse 
} from '../model/carrito.model';
import { BoletaRequest, BoletaResponse } from '../model/boleta.model';
import { environment } from '../../../environment';
import { TiendaService } from './tienda.service';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private apiUrl = `${environment.apiUrl}/api/public/carrito`;

  private carritoItems = new BehaviorSubject<CarritoItemResponse[]>([]);
  carritoItems$ = this.carritoItems.asObservable();

  private itemsCount = new BehaviorSubject<number>(0);
  itemsCount$ = this.itemsCount.asObservable();

  private totalPrecio = new BehaviorSubject<number>(0);
  totalPrecio$ = this.totalPrecio.asObservable();

  constructor(
    private http: HttpClient,
    private sessionService: CarritoSessionService,
    private tiendaService: TiendaService
  ) {
    // Carga inicial automática cuando se construye el servicio
    this.cargarCarritoDesdeBackend();
  }

  private getSessionId(): string {
    const sid = this.sessionService.getSessionId();
    if (!sid) {
      throw new Error('No hay sessionId disponible. Inicia sesión o recarga la página.');
    }
    return sid;
  }

  private getTiendaId(): number {
    const id = this.tiendaService.currentTiendaValue?.id;
    if (!id) {
      throw new Error('No hay tienda seleccionada. Por favor selecciona una tienda.');
    }
    return id;
  }

  cargarCarritoDesdeBackend(): void {
    const sessionId = this.getSessionId();
    const tiendaId = this.getTiendaId(); // ← ahora lanza error si no hay tienda

    this.http.get<CarritoItemResponse[]>(
      `${this.apiUrl}/session/${sessionId}?tiendaId=${tiendaId}`
    ).subscribe({
      next: (items) => this.actualizarEstadoCarrito(items || []),
      error: (err) => {
        console.error('Error al cargar carrito:', err);
        this.actualizarEstadoCarrito([]);
        // Opcional: mostrar alerta al usuario si es crítico
        // Swal.fire({ icon: 'error', title: 'Error', text: 'No pudimos cargar tu carrito' });
      }
    });
  }

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

 actualizarCantidad(itemId: number, cantidad: number, varianteId: number): Observable<CarritoItemResponse> {
  if (!Number.isInteger(itemId) || itemId <= 0) {
    return throwError(() => new Error('ID de carrito inválido'));
  }
  if (!Number.isInteger(varianteId) || varianteId <= 0) {
    return throwError(() => new Error('Variante ID inválido'));
  }

  const request = {
    sessionId: this.getSessionId(),
    varianteId: varianteId,      // ← ¡Esto era lo que faltaba!
    cantidad: cantidad
  };

  console.log(`Actualizando carrito ID ${itemId} → variante ${varianteId} → cantidad ${cantidad}`);

  return this.http.put<CarritoItemResponse>(`${this.apiUrl}/${itemId}`, request).pipe(
    tap(() => this.cargarCarritoDesdeBackend()),
    catchError(err => {
      console.error('Error PUT actualizar:', err);
      return throwError(() => err);
    })
  );
}

  eliminarItem(itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${itemId}`).pipe(
      tap(() => this.cargarCarritoDesdeBackend())
    );
  }

  vaciarCarrito(): Observable<void> {
    const sessionId = this.getSessionId();
    const tiendaId = this.getTiendaId();

    return this.http.delete<void>(`${this.apiUrl}/session/${sessionId}?tiendaId=${tiendaId}`).pipe(
      tap(() => this.actualizarEstadoCarrito([]))
    );
  }

  private actualizarEstadoCarrito(items: CarritoItemResponse[]): void {
    this.carritoItems.next(items);
    this.itemsCount.next(items.reduce((sum, item) => sum + (item.cantidad || 0), 0));
    this.totalPrecio.next(items.reduce((sum, item) => sum + ((item.precio || 0) * (item.cantidad || 0)), 0));
  }

  getTotalItemsSync(): number { return this.itemsCount.value; }
  getTotalPrecioSync(): number { return this.totalPrecio.value; }
  getItemsSync(): CarritoItemResponse[] { return this.carritoItems.value; }

  checkoutOnline(
    datosComprador: {
      compradorNombre: string;
      compradorEmail: string;
      compradorTelefono?: string;
      direccionEnvio: string;
      referenciaEnvio?: string;
      distrito: string;
      provincia: string;
      departamento: string;
      codigoPostal?: string;
      tipoEntrega: 'DOMICILIO' | 'RECOGIDA_EN_TIENDA' | 'AGENCIA';
    },
    userId?: number
  ): Observable<BoletaResponse> {
    const sessionId = this.getSessionId();
    const tiendaId = this.getTiendaId(); // ← obligatorio y validado

    const request: BoletaRequest = {
      sessionId,
      tiendaId,
      userId: userId ?? null,
      compradorNombre: datosComprador.compradorNombre,
      compradorEmail: datosComprador.compradorEmail,
      compradorTelefono: datosComprador.compradorTelefono,
      direccionEnvio: datosComprador.direccionEnvio,
      referenciaEnvio: datosComprador.referenciaEnvio,
      distrito: datosComprador.distrito,
      provincia: datosComprador.provincia,
      departamento: datosComprador.departamento,
      codigoPostal: datosComprador.codigoPostal,
      tipoEntrega: datosComprador.tipoEntrega
    };

    return this.http.post<BoletaResponse>(`${this.apiUrl}/checkout/online`, request).pipe(
      tap(() => this.cargarCarritoDesdeBackend()),
      catchError(err => {
        if (err.status === 400 && err.error?.error === 'MISSING_EMAIL_CONFIG') {
          return throwError(() => ({
            type: 'missing_email_config',
            message: err.error.message || 'Falta configurar tu correo y contraseña de aplicación'
          }));
        }
        return throwError(() => err);
      })
    );
  }

  checkoutWhatsapp(
    datosComprador: {
      compradorNombre: string;
      compradorEmail: string;
      compradorTelefono?: string;
      direccionEnvio: string;
      referenciaEnvio?: string;
      distrito: string;
      provincia: string;
      departamento: string;
      codigoPostal?: string;
      tipoEntrega: 'DOMICILIO' | 'RECOGIDA_EN_TIENDA' | 'AGENCIA';
    },
    userId?: number
  ): Observable<string> {
    const sessionId = this.getSessionId();
    const tiendaId = this.getTiendaId();

    const request: BoletaRequest = {
      sessionId,
      tiendaId,
      userId: userId ?? null,
      compradorNombre: datosComprador.compradorNombre,
      compradorEmail: datosComprador.compradorEmail,
      compradorTelefono: datosComprador.compradorTelefono,
      direccionEnvio: datosComprador.direccionEnvio,
      referenciaEnvio: datosComprador.referenciaEnvio,
      distrito: datosComprador.distrito,
      provincia: datosComprador.provincia,
      departamento: datosComprador.departamento,
      codigoPostal: datosComprador.codigoPostal,
      tipoEntrega: datosComprador.tipoEntrega
    };

    console.log('ENVIANDO checkoutWhatsapp:', JSON.stringify(request, null, 2));

    return this.http.post<string>(`${this.apiUrl}/checkout/whatsapp`, request, { responseType: 'text' as 'json' }).pipe(
      tap(() => this.cargarCarritoDesdeBackend())
    );
  }
}