// src/app/model/tienda-admin.model.ts
export interface TiendaResponse {
  id: number;
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapaUrl?: string;
  logoImgUrl?: string;
  activo: boolean;
  userId: number;
  userEmail?: string; // ← Ya lo tienes, bien
  // NUEVOS CAMPOS PARA LA SUSCRIPCIÓN ACTIVA
  planNombre?: string;          // Nombre del plan actual
  planSlug?: string;
  estadoSuscripcion?: string;   // 'trial', 'active', 'canceled', etc.
  trialEndsAt?: string;
  fechaFin?: string;
  maxProductos?: number;        // Opcional: límites del plan actual
  maxVariantes?: number;
}

export interface TiendaPage {
  content: TiendaResponse[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}