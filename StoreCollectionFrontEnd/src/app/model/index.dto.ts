// src/app/model/index.dto.ts

export interface AtributoValorPublic {
  atributoNombre: string;
  valor: string;
}

export interface VariantePublic {
  id: number;
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo: boolean;
  
  // ¡¡ CAMPOS NUEVOS que faltaban !!
  precio_anterior?: number | null;         // para ofertas (tachado)
  descripcion_corta?: string | null;       // descripción breve por variante
  
  atributos: AtributoValorPublic[];
}
export interface ProductoPublic {
  id: number;
  nombre: string;
  slug: string;
  nombreCategoria: string;
  precioMinimo: number;
  stockTotal: number;
  imagenPrincipal: string;
  // Campo NUEVO: descripción corta general del producto
  descripcion_corta?: string | null;
  activo: boolean;
  variantes: VariantePublic[];
}