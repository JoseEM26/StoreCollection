// src/app/model/categoria-admin.model.ts
export interface CategoriaResponse {
  id: number;
  nombre: string;
  slug: string;
  activo: boolean;         
  tiendaId: number;
}

export interface CategoriaRequest {
  nombre: string;
slug?: string;  
tiendaId?: number; 
}

export interface CategoriaPage {
  content: CategoriaResponse[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}