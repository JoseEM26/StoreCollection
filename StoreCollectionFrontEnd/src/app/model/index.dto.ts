// src/app/model/index.dto.ts

export interface AtributoValorPublic {
  atributoNombre: string;
  valor: string;
}

export interface VariantePublic {
  id: number;                    // ← Ahora obligatorio
  precio: number;
  stock: number;
  imagenUrl?: string;
  activo: boolean;
  atributos: AtributoValorPublic[]; // ← Nueva propiedad
}

export interface ProductoPublic {
  id: number;
  nombre: string;
  slug: string;
  nombreCategoria: string;
  precioMinimo: number;
  stockTotal: number;
  imagenPrincipal: string;
  activo: boolean;
  variantes: VariantePublic[];
}