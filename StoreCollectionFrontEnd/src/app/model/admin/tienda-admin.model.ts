// src/app/model/tienda-admin.model.ts (o un archivo central de modelos de tienda)

// Interfaz base compartida (opcional, para evitar duplicación)
interface TiendaBase {
  id: number;
  nombre: string;
  slug: string;
  whatsapp?: string;
  moneda: 'SOLES' | 'DOLARES';
  descripcion?: string;
  direccion?: string;
  horarios?: string;
  mapa_url?: string;           // ← Mantener snake_case para coincidir con backend
  logo_img_url?: string;       // ← Igual aquí
  activo?: boolean;
  userId?: number;
  userEmail?: string;
fechaVencimiento?: string | null;  
ruc?: string | null;  
}

// Para respuestas del admin/owner (más datos sensibles y de suscripción)
export interface TiendaResponse extends TiendaBase {
  activo: boolean;             // Obligatorio en admin
  userId: number;              // Obligatorio en admin
  userEmail?: string;

  // Datos del plan y suscripción actual
  planNombre?: string;
  planSlug?: string;
  // Información del plan actual
  planId: number;
  estadoSuscripcion?: string;  // ej: 'active', 'trialing', 'canceled', 'past_due'
  trialEndsAt?: string | null; // ISO date string o null
  fechaFin?: string | null;
  maxProductos?: number;
  maxVariantes?: number;
  tiktok?: string | null;
  instagram?: string | null;
  facebook?: string | null;
ruc?: string | null;  

}

// Para vistas públicas (menos datos, más restringido)
export interface TiendaPublic extends TiendaBase {
  planNombre: 'Gratis' | 'Básico' | 'Pro' | 'Enterprise'; // Puedes mantenerlo restringido si quieres
  logo_img_url?: string;
}

// Paginación genérica (recomendado tener un modelo base)
export interface Page<T> {
  content: T[];
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
  sort: { sorted: boolean; unsorted: boolean; empty: boolean };
  empty: boolean;
}

// Tipos finales para usar en servicios
export type TiendaAdminPage = Page<TiendaResponse>;
export type TiendaPublicPage = Page<TiendaPublic>;