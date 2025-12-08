export interface ProductoPublic {
  id: number;
  nombre: string;
  slug: string;
  nombreCategoria: string;
  precioMinimo: number;        
  stockTotal: number;
  imagenPrincipal: string;
  variantes: {
    precio: number;
    stock: number;
    imagenUrl: string;
    activo: boolean;
  }[];
}

export interface TiendaDropdown {
  id: number;
  nombre: string;
}