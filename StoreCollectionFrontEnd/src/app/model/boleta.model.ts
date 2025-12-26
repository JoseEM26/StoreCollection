// src/app/model/boleta.model.ts (COMPLETO y compatible con backend)

import { AtributoResponse } from './carrito.model';

export interface BoletaRequest {
  sessionId: string;
  tiendaId: number;
  userId?: number | null;  // opcional

  // Datos del comprador (obligatorios)
  compradorNombre: string;
  compradorEmail: string;
  compradorTelefono?: string;  // opcional

  // Datos de envío (obligatorios excepto algunos)
  direccionEnvio: string;
  referenciaEnvio?: string;
  distrito: string;
  provincia: string;
  departamento: string;
  codigoPostal?: string;

  // Tipo de entrega
  tipoEntrega: 'DOMICILIO' | 'RECOGIDA_EN_TIENDA' | 'AGENCIA';

  // Opcional: método de pago (si lo implementas después)
  metodoPago?: string;
}

export interface BoletaResponse {
  id: number;
  sessionId: string;
  userId: number | null;
  tiendaId: number;
  total: number;
  fecha: string;
  estado: string;

  // Nuevos campos agregados
  compradorNombre: string;
  compradorEmail: string;
  compradorTelefono?: string;

  direccionEnvio: string;
  referenciaEnvio?: string;
  distrito: string;
  provincia: string;
  departamento: string;
  codigoPostal?: string;

  tipoEntrega: string;  // 'DOMICILIO' | etc...

  detalles: BoletaDetalleResponse[];
}
// src/app/model/boleta-detalle.model.ts (agregado para completar BoletaResponse, basado en backend)

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