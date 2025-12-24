// src/app/model/boleta.model.ts

// Para solicitudes de creación de boleta
export interface BoletaRequest {
  sessionId: string;
  tiendaId: number;
  userId?: number;
}

// Detalle de cada ítem en la boleta
export interface BoletaDetalleResponse {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;      // number (viene como double desde backend)
  subtotal: number;
  nombreProducto: string;
  sku?: string;
  imagenUrl?: string;
  atributos?: any[];           // opcional
}

// Respuesta individual de boleta
export interface BoletaResponse {
  id: number;
  sessionId: string;
  userId?: number;
  tiendaId: number;
  total: number;
  fecha: string;               // ISO string o formato legible
  estado: string;
  detalles: BoletaDetalleResponse[];
}

// Respuesta paginada (para listados admin/owner)
export interface BoletaPageResponse {
  content: BoletaResponse[];
  totalElements: number;
  totalPages: number;
  number: number;              // página actual (0-based)
  size: number;
  first: boolean;
  last: boolean;
}