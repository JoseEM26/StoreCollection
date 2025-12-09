export interface ProductoResponse {
  id: number;
  nombre: string;
  slug: string;
  categoriaId: number;
  categoriaNombre: string;
  tiendaId: number;
  tiendaSlug: string;
  precioMinimo: number;
  stockTotal: number;
  imagenPrincipal?: string;
  totalVariantes: number;
  variantesActivas: number;
  variantes: VarianteResponse[];
}
export interface VarianteResponse {
  id: number;
  sku: string;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo: boolean;
  atributos: AtributoSimple[];
}

export interface AtributoSimple {
  nombreAtributo: string;
  valor: string;
}
export interface ProductoPage {
  content: ProductoResponse[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}