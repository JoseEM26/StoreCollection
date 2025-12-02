export interface ProductoPublic {
  id: number;
  nombre: string;
  slug: string;
  nombreCategoria: string;        // ← este campo viene del backend
  precioMinimo: number;           // ← este también
  imagenPrincipal: string;        // ← y este
  stockTotal: number;             // ← y este
  variantes?: {
    precio: number;
    stock: number;
    imagenUrl: string;
    activo: boolean;
  }[];
}