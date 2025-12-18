// src/app/model/admin/producto-admin.model.ts

// === Respuesta del backend ===
export interface ProductoResponse {
  id: number;
  nombre: string;
  slug: string;
  categoriaId: number;
  categoriaNombre: string;
  tiendaId: number;
  activo: boolean;
  variantes: VarianteResponse[];
}

// === Para enviar al crear o actualizar ===
export interface ProductoRequest {
  nombre: string;
  slug: string;
  categoriaId: number;
  tiendaId?: number;           // Solo lo envía ADMIN en creación
  variantes?: VarianteRequest[]; // Opcional si no hay variantes
}

// === Variante ===
export interface VarianteResponse {
  id?: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  atributos: AtributoValorResponse[];
}

export interface VarianteRequest {
  id?: number;                 // Solo presente en edición
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  atributos?: AtributoValorRequest[];
}

// === Atributo y Valor (para variantes) ===
export interface AtributoValorResponse {
  id: number;
  atributoNombre: string;
  valor: string;
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