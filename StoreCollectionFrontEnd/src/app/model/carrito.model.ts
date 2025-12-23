// src/app/model/carrito.model.ts

// Atributo individual (como lo devuelve el backend)
export interface AtributoResponse {
  id: number;
  nombre: string;      // ej: "Color", "Talla"
  valor: string;       // ej: "Rojo", "M"
  tiendaId: number;
}

// Modelo exacto de un ítem del carrito (coincide con CarritoResponse del backend)
export interface CarritoItemResponse {
  id: number;
  sessionId: string;
  cantidad: number;
  varianteId: number;

  // Datos enriquecidos del producto/variante
  nombreProducto: string;
  sku: string;
  precio: number;                  // double del backend → number en TS
  imagenUrl: string | null;

  // Ahora es una lista de objetos, no un string
  atributos: AtributoResponse[] | null;
}

// Request para agregar o actualizar
export interface CarritoRequest {
  sessionId: string;
  varianteId: number;
  cantidad: number;
}

// El endpoint GET /session/{sessionId} devuelve directamente un array
export type CarritoResponse = CarritoItemResponse[];