// src/app/model/boleta.model.ts

export interface BoletaRequest {
  sessionId: string;
  tiendaId: number;
  userId?: number; // Opcional si el usuario está logueado
}

export interface BoletaDetalleResponse {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;   // Viene como Double o BigDecimal → number en TS
  subtotal: number;
}

export interface BoletaResponse {
  id: number;
  sessionId: string;
  userId?: number;
  tiendaId: number;
  total: number;            // BigDecimal → convertido a number en backend (doubleValue)
  fecha: string;            // ISO string desde LocalDateTime
  estado: string;
  detalles: BoletaDetalleResponse[];
}