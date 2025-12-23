// src/app/model/admin/plan-admin.model.ts

export interface PlanResponse {
  id: number;
  nombre: string;
  precio: number; // BigDecimal se mapea a number en JSON
  maxProductos: number;
  mesInicio: number;
  mesFin: number;
  activo: boolean;  // ← Asegúrate de tener este campo
}

export interface PlanRequest {
  nombre: string;
  precio: number;
  maxProductos: number;
  mesInicio: number;
  mesFin: number;
}

export interface PlanPage {
  content: PlanResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}