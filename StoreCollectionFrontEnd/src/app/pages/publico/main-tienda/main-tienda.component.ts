// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductosService } from '../../../service/productos.service';
import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';

@Component({
  selector: 'app-main-tienda',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductoCardComponent],
  templateUrl: './main-tienda.component.html',
  styleUrls: ['./main-tienda.component.css']
})
export class MainTiendaComponent {
  destacados: any[] = [];

  categorias = [
    { 
      nombre: 'Celulares', 
      slug: 'celulares', 
      image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80' 
    },
    { 
      nombre: 'Moda y Ropa', 
      slug: 'moda', 
      image: 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=800&q=80' 
    },
    { 
      nombre: 'Laptops', 
      slug: 'laptops', 
      image: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80' 
    },
    { 
      nombre: 'Electrónica', 
      slug: 'electronica', 
      image: 'https://images.unsplash.com/photo-1588872657578-39ef887260c0?w=800&q=80' 
    },
    { 
      nombre: 'Hogar', 
      slug: 'hogar', 
      image: 'https://images.unsplash.com/photo-1618221195710-dd9247aaff8?w=800&q=80' 
    },
    { 
      nombre: 'Deportes', 
      slug: 'deportes', 
      image: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800&q=80' 
    },
    { 
      nombre: 'Belleza', 
      slug: 'belleza', 
      image: 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=800&q=80' 
    },
    { 
      nombre: 'Juguetes', 
      slug: 'juguetes', 
      image: 'https://images.unsplash.com/photo-1515488042361-ee00e0ddd4e4?w=800&q=80' 
    }
  ];

  constructor(private service: ProductosService) {
    this.destacados = this.service.getDestacados();
  }
}