export interface TiendaPublic {
  id: number;
  nombre: string;
  slug: string;
  whatsapp: string;
  moneda: string;
  descripcion: string;
  direccion?: string;
  horarios?: string;
  planId?: number;
  planNombre: string;        // ← este viene del backend
  userId: number;
  userEmail: string;         // ← este también viene del backend
}

import { Page } from './index';

export type TiendaPage = Page<TiendaPublic>;