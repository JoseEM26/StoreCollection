// src/app/model/tienda-admin.model.ts
export interface TiendaResponse {
  id: number;
  nombre: string;
  slug: string;
  whatsapp: string;
  moneda: string;
  descripcion: string;
  direccion: string;
  horarios: string;
  mapa_url: string;
  logo_img_url: string;
  planId: number;
  planNombre: string;
  userId: number;
  userEmail: string;
  activo: boolean;
}

export interface TiendaPage {
  content: TiendaResponse[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
    sort: any;
  };
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}