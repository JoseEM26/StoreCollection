// src/app/models/producto.model.ts
export interface Producto {
  id: number;
  nombre: string;
  precio: number;
  descripcion: string;
  categoria: string;
  imagen: string;
  destacado?: boolean;
  stock:number;
}