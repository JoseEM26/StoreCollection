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
  variantes: VarianteResponse[] | null; // Permite null si no hay variantes
}

// === Request para crear o actualizar ===
export interface ProductoRequest {
  nombre: string;
  slug: string;
  categoriaId: number;
  tiendaId?: number;     // Opcional: solo lo envía el ADMIN
  activo: boolean;
  variantes: VarianteRequest[];
}

// === Variante ===
export interface VarianteResponse {
  id: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;           // también aquí usa undefined, no null
  activo: boolean;
  atributos: AtributoValorResponse[];
}

export interface VarianteRequest {
  id?: number;
  sku: string;
  precio: number;
  stock: number;
  imagen?: File;                
  imagenUrl?: string;          
  activo?: boolean;
  atributos: AtributoValorRequest[];
}
export interface AtributoValorResponse {
  id: number;              // ID del AtributoValor
  atributoNombre: string;  // Nombre del atributo (ej: "Color")
  valor: string;           // Valor (ej: "Rojo")
}

// Para dropdowns de atributos (si en el futuro lo necesitas)
export interface AtributoConValores {
  id: number;
  descripcion: string;
  valores: DropTownStandar[]; // { id: atributoValorId, descripcion: "Rojo" }
}

export interface AtributoValorRequest {
  atributoNombre: string;
  valor: string;

  // Campos temporales para crear nuevos atributos/valores (NO se envían al backend)
  atributoNombreTemp?: string;
  valorTemp?: string;
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