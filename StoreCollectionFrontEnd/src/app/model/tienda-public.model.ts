// src/app/model/tienda-public.model.ts
import { Page } from './index';

export interface TiendaPublic {
  id: number;
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  planId?: number;
  planNombre: 'Gratis' | 'Básico' | 'Pro' | 'Enterprise';
  userId?: number;
  userEmail?: string;
}

/** Página de tiendas públicas */
export type TiendaPage = Page<TiendaPublic>;