// src/app/model/index.dto.ts
export interface ProductoPublic {
  id: number;
  nombre: string;
  slug: string;
  nombreCategoria: string;
  precioMinimo: number;        // ← viene como number (Java BigDecimal → JS number)
  stockTotal: number;
  imagenPrincipal: string;
  activo:boolean;
  variantes: {
    precio: number;
    stock: number;
    imagenUrl: string;
    activo: boolean;
  }[];
}