// src/app/model/boleta.model.ts (COMPLETO y compatible con backend)

import { AtributoResponse } from './carrito.model';

export interface BoletaRequest {
  sessionId: string;
  tiendaId: number;
  userId?: number | null;  // Compatible con Integer null del backend
}

export interface BoletaResponse {
  id: number;
  sessionId: string;
  userId: number | null;
  tiendaId: number;
  total: number;  // BigDecimal → number en TS
  fecha: string;
  estado: string;
  detalles: BoletaDetalleResponse[];
}
// src/app/model/boleta-detalle.model.ts (agregado para completar BoletaResponse, basado en backend)

export interface BoletaDetalleResponse {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;  // BigDecimal → number
  subtotal: number;  // BigDecimal → number
  nombreProducto: string;
  sku: string | null;
  imagenUrl: string | null;
  atributos: AtributoResponse[] | null;  // Reutiliza de carrito.model
}

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