// src/app/model/index.ts
// MODELOS 100% COMPATIBLES CON TU BACKEND JAVA (StoreCollection 2025)

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  password?: string; // solo en creación/login
  celular?: string;
  rol: 'ADMIN' | 'OWNER' | 'CUSTOMER';
  createdAt?: string;
  updatedAt?: string;
}

export interface Plan {
  id: number;
  nombre: string;
  precio: number;
  maxProductos: number;
  mesInicio: number;  // ← coincide con mes_inicio en BD
  mesFin: number;     // ← coincide con mes_fin en BD
}

export interface Tienda {
  id: number;
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapaUrl?: string;
  plan?: Plan | null;
  planId?: number;
  user?: Usuario | null;
  userId?: number;
  createdAt?: string;
  updatedAt?: string;
  logo_img_url?:string;
  mapa_url?:string;
}

export interface Categoria {
  id: number;
  nombre: string;
  slug: string;
  tiendaId: number;
  tienda?: Tienda;
}

// export interface Atributo {
//   id: number;
//   nombre: string; // ej: "Talla", "Color"
//   tiendaId: number;
//   tienda?: Tienda;
//   valores?: AtributoValor[]; // opcional para cargar con atributos
// }

// export interface AtributoValor {
//   id: number;
//   valor: string;     // ej: "Rojo", "38", "65W"
//   atributoId: number;
//   atributo?: Atributo;
//   tiendaId: number;
//   tienda?: Tienda;
// }

// export interface Producto {
//   id: number;
//   nombre: string;
//   slug: string;
//   categoriaId: number;
//   categoria?: Categoria;
//   tiendaId: number;
//   tienda?: Tienda;
//   variantes?: Variante[];
//   createdAt?: string;
//   updatedAt?: string;
// }

// export interface Variante {
//   id: number;
//   productoId: number;
//   producto?: Producto;
//   tiendaId: number;
//   tienda?: Tienda;
//   sku: string;
//   precio: number;
//   stock: number;
//   imagenUrl?: string | null;  // ← ¡¡IMPORTANTE!! coincide con imagen_url en BD
//   activo: boolean;
//   atributos?: AtributoValor[]; // relación muchos-a-muchos (Variante_Atributo)
//   createdAt?: string;
//   updatedAt?: string;
// }

// export interface CarritoItem {
//   id: number;
//   sessionId: string;
//   varianteId: number;
//   variante?: Variante;
//   cantidad: number;
//   createdAt?: string;
// }

// ========================================
// DTOs PARA FORMULARIOS Y PETICIONES
// ========================================

export interface CrearTiendaDTO {
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapaUrl?: string;
  planId: number;
}

export interface CrearProductoDTO {
  nombre: string;
  slug: string;
  categoriaId: number;
}

export interface CrearVarianteDTO {
  productoId: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo?: boolean;
  atributoValorIds: number[]; // IDs de los AtributoValor seleccionados
}

export interface CrearCategoriaDTO {
  nombre: string;
  slug: string;
}

export interface CrearAtributoDTO {
  nombre: string;
}

export interface CrearAtributoValorDTO {
  valor: string;
  atributoId: number;
}

// ========================================
// RESPUESTAS PAGINADAS (Spring Data)
// ========================================

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;
}

// ========================================
// ENUMS / CONSTANTES
// ========================================

export const ROLES = ['ADMIN', 'OWNER', 'CUSTOMER'] as const;
export type Rol = typeof ROLES[number];

export const MONEDAS = ['SOLES', 'DOLARES'] as const;
export type Moneda = typeof MONEDAS[number];

export const ESTADOS_VARIANTE = ['ACTIVO', 'INACTIVO'] as const;