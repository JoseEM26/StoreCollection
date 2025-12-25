// src/app/model/carrito.model.ts (COMPLETO y compatible con backend)

export interface AtributoResponse {
  id: number;
  nombre: string;
  valor: string;
  tiendaId: number;
}

export interface CarritoItemResponse {
  id: number;
  sessionId: string;
  cantidad: number;
  varianteId: number;
  nombreProducto: string;
  sku: string | null;
  precio: number;  // Double del backend → number en TS
  imagenUrl: string | null;
  atributos: AtributoResponse[] | null;
}

export interface CarritoRequest {
  sessionId: string;
  varianteId: number;
  cantidad: number;
}

export type CarritoResponse = CarritoItemResponse[];