// src/app/model/admin/producto-admin.model.ts

import { DropTownStandar } from "../../service/droptown.service";

// === Respuesta del backend ===
export interface ProductoResponse {
  id: number;
  nombre: string;
  slug: string;
  categoriaId: number;
  categoriaNombre: string;
  tiendaId: number;
  activo: boolean;
variantes: VarianteResponse[] | null;  // ← Permite null
}

export interface ProductoRequest {
  nombre: string;
  slug: string;
  categoriaId: number;
  tiendaId?: number;         // solo admin
  variantes: VarianteRequest[];
    activo: boolean;

}

export interface VarianteResponse {
  id: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo: boolean;
  atributos: AtributoValorResponse[];
}

export interface VarianteRequest {
  id?: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  atributos: AtributoValorRequest[];
  activo: Boolean;
}
// === Atributo y Valor (para variantes) ===
export interface AtributoValorResponse {
  id: number;                // atributoValorId
  atributoId: number;
  atributoNombre: string;
  valor: string;
}
export interface AtributoConValores {
  id: number;
  nombre: string;
  valores: DropTownStandar[];  // { id: atributoValorId, descripcion: "Rojo" }
}
export interface AtributoValorRequest {
  atributoNombre: string;      // Ej: "Color", "Talla"
  valor: string;               // Ej: "Rojo", "M"
}

// === Paginación ===
export interface ProductoPage {
  content: ProductoResponse[];
  pageable: {
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;
  empty: boolean;
}