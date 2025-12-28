// src/app/model/admin/plan-admin.model.ts
export interface PlanResponse {
  id: number;
  nombre: string;
  slug: string;
  descripcion?: string | null;
  precioMensual: number;
  precioAnual?: number | null;        // ← Añade | null
  intervaloBilling: string;
  intervaloCantidad: number;
  duracionDias?: number | null;       // ← Añade | null
  maxProductos: number;
  maxVariantes: number;
  esTrial: boolean;
  diasTrial: number;
  esVisiblePublico: boolean;
  orden: number;
  activo: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PlanRequest {
  nombre: string;
  slug: string;
  descripcion?: string | null;
  precioMensual: number;
  precioAnual?: number | null;        // ← Añade | null
  intervaloBilling: string;
  intervaloCantidad: number;
  duracionDias?: number | null;       // ← Añade | null
  maxProductos: number;
  maxVariantes: number;
  esTrial?: boolean;
  diasTrial?: number;
  esVisiblePublico?: boolean;
  orden?: number;
  activo?: boolean;
}

export interface PlanPage {
  content: PlanResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}