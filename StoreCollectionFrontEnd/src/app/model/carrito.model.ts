// src/app/model/carrito.model.ts

// Modelo exacto que coincide con CarritoResponse del backend
export interface CarritoItemResponse {
  id: number;
  sessionId: string;
  cantidad: number;
  varianteId: number;

  // Campos agregados en el backend para mostrar en el carrito
  nombreProducto: string;
  sku: string;
  precio: number;           // Double del backend
  imagenUrl: string | null;
  atributos: string | null; // Ej: "Rojo, Talla M"
}

// Request para agregar/actualizar item
export interface CarritoRequest {
  sessionId: string;
  varianteId: number;
  cantidad: number;
}

// El endpoint GET /session/{id} devuelve un array de CarritoItemResponse
export type CarritoResponse = CarritoItemResponse[];