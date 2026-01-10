// src/app/model/boleta.model.ts (COMPLETO y compatible con backend)

import { AtributoResponse } from './carrito.model';

export interface BoletaRequest {
  sessionId: string;
  tiendaId: number;
  userId?: number | null;  // opcional

  // Datos del comprador (obligatorios)
  compradorNombre: string;
    compradorNumero: string;

}
// =============================================
// Request para crear venta directa (mostrador)
// =============================================
export interface VentaDirectaRequest {
  tiendaId: number;
  compradorNombre?: string;
  compradorEmail?: string | null;
  compradorNumero?: string | null;   // ← cambiar de compradorTelefono
  items: VentaDirectaItemRequest[];
}

export interface VentaDirectaItemRequest {
  varianteId: number;
  cantidad: number;
}
export interface BoletaResponse {
  id: number;
  sessionId: string | null;
  userId: number | null;
  tiendaId: number;
  total: number;
  fecha: string;
  estado: string;

  compradorNombre: string | null;
  compradorNumero: string | null;
  compradorEmail?: string | null;     // si lo agregaste

  // Dirección y entrega (agregar estos)
  direccionEnvio?: string | null;
  referenciaEnvio?: string | null;
  distrito?: string | null;
  provincia?: string | null;
  departamento?: string | null;
  codigoPostal?: string | null;
  tipoEntrega?: 'DOMICILIO' | 'RECOGIDA_EN_TIENDA' | 'AGENCIA' | null;

  detalles: BoletaDetalleResponse[];
}

export interface BoletaDetalleResponse {
  id: number;
  varianteId: number;
  cantidad: number;
  precioUnitario: number;  // BigDecimal → number
  subtotal: number;  // BigDecimal → number
  nombreProducto: string;
  sku: string | null;
  imagenUrl: string | null;
  atributos: AtributoResponse[] | null;  // Reutiliza de carrito.model
}

export interface BoletaPageResponse {
  content: BoletaResponse[];         // lista de boletas
  totalElements: number;             // total de registros
  totalPages: number;                // total de páginas
  number: number;                    // página actual (0-based)
  size: number;                      // tamaño de página
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}