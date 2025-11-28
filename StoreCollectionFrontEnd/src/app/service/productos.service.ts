// src/app/services/productos.service.ts
import { Injectable } from '@angular/core';
import { Producto } from '../model/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosService {
  private productos: Producto[] = [
    // === CELULARES ===
    { 
      id: 1, 
      nombre: 'iPhone 16 Pro Max 512GB Desert Titanium', 
      precio: 1499, 
      descripcion: 'Chip A18 Pro, cámara 48MP Fusion, titanio grado 5.', 
      categoria: 'Celulares', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_825978-MLA74720848263_032024-F.webp', 
      destacado: true, 
      stock: 8 
    },
    { 
      id: 2, 
      nombre: 'Samsung Galaxy S24 Ultra 512GB Titanium Gray', 
      precio: 1299, 
      descripcion: 'S Pen integrado, cámara 200MP, IA Galaxy.', 
      categoria: 'Celulares', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_687957-MLA74999712554_032024-F.webp', 
      destacado: true, 
      stock: 12 
    },
    { 
      id: 3, 
      nombre: 'Xiaomi 14 Ultra 16GB + 512GB Black', 
      precio: 1099, 
      descripcion: 'Cámara Leica Summilux, Snapdragon 8 Gen 3.', 
      categoria: 'Celulares', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_809441-MLA74738369682_032024-F.webp', 
      stock: 15 
    },

    // === COMPUTADORAS ===
    { 
      id: 4, 
      nombre: 'Apple MacBook Pro 16" M3 Max 48GB 1TB Space Black', 
      precio: 3999, 
      descripcion: 'El Mac más potente jamás creado.', 
      categoria: 'Computadoras', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_959684-MLA75031897246_032024-F.webp', 
      destacado: true, 
      stock: 5 
    },
    { 
      id: 5, 
      nombre: 'Laptop Gamer ASUS ROG Strix G16 RTX 4070 i9', 
      precio: 2199, 
      descripcion: '165Hz QHD, RGB, refrigeración líquida metálica.', 
      categoria: 'Computadoras', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_908511-MLA74378859741_022024-F.webp', 
      destacado: true, 
      stock: 7 
    },

    // === ROPA ===
    { 
      id: 7, 
      nombre: 'Camiseta Oversize Algodón 100% Premium Negra', 
      precio: 39, 
      descripcion: '240g, costuras reforzadas, unisex.', 
      categoria: 'Ropa', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_793682-MLA78945612301_092024-F.webp', 
      destacado: true, 
      stock: 150 
    },
    { 
      id: 10, 
      nombre: 'Buzo Hoodie Oversize Canguro Algodón con Felpa', 
      precio: 69, 
      descripcion: 'Calidad premium, capucha doble, bolsillo grande.', 
      categoria: 'Ropa', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_667890-MLA78912398765_092024-F.webp', 
      destacado: true, 
      stock: 95 
    },

    // === ELECTRÓNICA ===
    { 
      id: 11, 
      nombre: 'Sony WH-1000XM5 Cancelación de Ruido', 
      precio: 399, 
      descripcion: 'Mejor ANC del mundo, 30h batería, negro.', 
      categoria: 'Electrónica', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_912345-MLA52345678912_112023-F.webp', 
      destacado: true, 
      stock: 20 
    },
    { 
      id: 13, 
      nombre: 'PlayStation 5 Pro 2TB SSD Digital Edition', 
      precio: 799, 
      descripcion: 'GPU mejorada, ray tracing avanzado, 8K.', 
      categoria: 'Electrónica', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_890123-MLA78945612345_092024-F.webp', 
      destacado: true, 
      stock: 3 
    },

    // === BELLEZA ===
    { 
      id: 17, 
      nombre: 'Creed Aventus EDP 100ml Original', 
      precio: 379, 
      descripcion: 'Fragancia icónica, batch 2024, máxima duración.', 
      categoria: 'Belleza', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_890123-MLA52378945612_112023-F.webp', 
      destacado: true, 
      stock: 9 
    },
    { 
      id: 18, 
      nombre: 'Dyson Airwrap Complete Long Blue/Copper', 
      precio: 599, 
      descripcion: 'Edición 2025, 6 accesorios, tecnología Coanda.', 
      categoria: 'Belleza', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_667890-MLA74567890123_022024-F.webp', 
      stock: 6 
    },

    // === JUGUETES ===
    { 
      id: 21, 
      nombre: 'LEGO Technic 42115 Lamborghini Sián FKP 37', 
      precio: 449, 
      descripcion: '3.696 piezas, escala 1:8, puertas tijera funcionales.', 
      categoria: 'Juguetes', 
      imagen: 'https://http2.mlstatic.com/D_NQ_NP_2X_756432-MLA46345678901_062024-F.webp', 
      destacado: true, 
      stock: 7 
    },
    { 
      id: 22, 
      nombre: 'Nintendo Switch OLED Edición Mario Red', 
      precio: 499, 
      descripcion: 'Pantalla OLED 7", Joy-Con rojos, dock especial.', 
      categoria: 'Juguetes', 
      imagen: 'https://http2.mlstatic.com/D_612345-MLA74512345678_032024-F.jpg', 
      stock: 16 
    },
  ];

  getAll(): Producto[] {
    return this.productos;
  }

  getById(id: number): Producto | undefined {
    return this.productos.find(p => p.id === id);
  }

  getByCategoria(categoria: string): Producto[] {
    return this.productos.filter(p => p.categoria.toLowerCase() === categoria.toLowerCase());
  }

  getDestacados(): Producto[] {
    return this.productos.filter(p => p.destacado);
  }

  hayStock(id: number): boolean {
    const p = this.getById(id);
    return p ? p.stock > 0 : false;
  }
}