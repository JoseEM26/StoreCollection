// src/app/model/boleta-admin.model.ts

export interface BoletaDetalleResponse {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  nombreProducto: string;
  sku: string;
  imagenUrl?: string | null;
  atributos?: AtributoResponse[] | null;
}

export interface AtributoResponse {
  id: number;
  nombre: string;
  valor: string;
  tiendaId: number;
}

export interface BoletaResponse {
  id: number;
  sessionId: string;
  userId?: number;
  tiendaId: number;
  total: number;
  fecha: string; // ISO date string
  estado: 'PENDIENTE' | 'ATENDIDA' | 'CANCELADA';
  detalles: BoletaDetalleResponse[];
}