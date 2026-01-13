// src/app/model/admin/producto-admin.model.ts

import { DropTownStandar } from "../../service/droptown.service";

// === ITEM PARA LA LISTA ADMINISTRATIVA (coincide exactamente con el DTO del backend) ===
export interface ProductoAdminListItem {
  id: number;
  nombre: string;
  slug: string;
  categoriaId: number;
  categoriaNombre: string;
  tiendaId: number;
  tiendaNombre: string;
  activo: boolean;
  precioMinimo: number;
  precioMaximo: number;
  stockTotal: number;
  imagenPrincipal: string;
  tieneVariantes: boolean;
  cantidadVariantes: number;
  variantes?: VarianteResponse[] | null;
}

// === Página para la lista administrativa ===
export interface ProductoAdminListPage {
  content: ProductoAdminListItem[];
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

// === Respuesta detallada de un producto individual (para edición, creación, etc.) ===
export interface ProductoResponse {
  id: number;
  nombre: string;
  slug: string;
  categoriaId: number;
  categoriaNombre: string;
  tiendaId: number;
  activo: boolean;
  variantes: VarianteResponse[] | null;
}

// === Request para crear o actualizar ===
export interface ProductoRequest {
  nombre: string;
  slug: string;
  categoriaId: number;
  tiendaId?: number;
  activo?: boolean;                           // ← Corregido: era obligatorio, ahora opcional (backend lo maneja)
  variantes: VarianteRequest[];
}

// === Variante ===
export interface VarianteResponse {
  id: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string | null;
  activo: boolean;
  precio_anterior?: number | null;            // ← NUEVO: precio anterior para ofertas
  descripcion_corta?: string | null;          // ← NUEVO: descripción corta de la variante
  atributos: AtributoValorResponse[];
}

export interface VarianteRequest {
  id?: number;
  sku: string;
  precio: number;
  stock: number;
  imagen?: File;
  imagenUrl?: string | null;
  activo?: boolean;
  precio_anterior?: number | null;            // ← NUEVO
  descripcion_corta?: string | null;          // ← NUEVO
  atributos: AtributoValorRequest[];
}

export interface AtributoValorResponse {
  id: number;
  atributoNombre: string;
  valor: string;
}

export interface AtributoConValores {
  id: number;
  descripcion: string;
  valores: DropTownStandar[];
}

export interface AtributoValorRequest {
  atributoNombre: string;
  valor: string;
  atributoNombreTemp?: string;
  valorTemp?: string;
}

// === Página antigua (para otras vistas si aún la usas) ===
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