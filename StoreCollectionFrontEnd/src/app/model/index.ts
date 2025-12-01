// src/app/model/index.ts

// 1. Usuario
export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  password?: string; // solo en creación
  celular?: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
}

// 2. Tienda
export interface Tienda {
  id: number;
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  plan?: Plan | null;
  user?: Usuario | null;
}

// 3. Plan
export interface Plan {
  id: number;
  nombre: string;
  precio: number;
  maxProductos: number;
  mesInicio: number;
  mesFin: number;
}

// 4. Categoria
export interface Categoria {
  id: number;
  nombre: string;
  slug: string;
  tienda: Tienda;
  tiendaId: number;
}

// 5. Atributo (ej: Color, Talla, Material)
export interface Atributo {
  id: number;
  nombre: string; // "Color", "Talla", etc.
  tienda: Tienda;
  tiendaId: number;
}

// 6. AtributoValor (ej: "Rojo", "M", "Algodón")
export interface AtributoValor {
  id: number;
  valor: string; // "Rojo", "L", "Blanco"
  atributo: Atributo;
  atributoId: number;
  tienda: Tienda;
  tiendaId: number;
}

// 7. Producto
export interface Producto {
  id: number;
  nombre: string;
  slug: string;
  categoria: Categoria;
  categoriaId: number;
  tienda: Tienda;
  tiendaId: number;
  variantes?: Variante[];
}

// 8. Variante (SKU específico con combinaciones de atributos)
export interface Variante {
  id: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo: boolean;
  producto: Producto;
  productoId: number;
  tienda: Tienda;
  tiendaId: number;
  atributos: AtributoValor[]; // ej: [Color: Rojo, Talla: M]
}

// 9. Carrito Item
export interface CarritoItem {
  id: number;
  sessionId: string;
  variante: Variante;
  varianteId: number;
  cantidad: number;
}

// 10. DTOs útiles para formularios
export interface CrearProductoDTO {
  nombre: string;
  slug: string;
  categoriaId: number;
  tiendaId: number;
}

export interface CrearVarianteDTO {
  productoId: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo?: boolean;
  atributoValorIds: number[]; // ids de AtributoValor seleccionados
}

export interface CrearAtributoDTO {
  nombre: string;
  tiendaId: number;
}

export interface CrearAtributoValorDTO {
  valor: string;
  atributoId: number;
  tiendaId: number;
}

// 11. Respuestas paginadas (para listas grandes)
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// 12. Enums para formularios
export const Roles = ['ADMIN', 'OWNER', 'CUSTOMER'] as const;
export const Monedas = ['SOLES', 'DOLARES'] as const;