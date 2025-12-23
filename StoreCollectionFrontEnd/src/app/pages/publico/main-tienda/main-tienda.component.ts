// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { Categoria } from '../../../model';
import { ProductoPublic } from '../../../model/index.dto';
import { TiendaService } from '../../../service/tienda.service';
import { TiendaPublicService } from '../../../service/tienda-public.service';
import { CategoriaPublicService } from '../../../service/categoria-public.service';
import { ProductoPublicService } from '../../../service/producto-public.service';

@Component({
  selector: 'app-main-tienda',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductoCardComponent],
  templateUrl: './main-tienda.component.html',
  styleUrls: ['./main-tienda.component.css']
})
export class MainTiendaComponent implements OnInit {
  tienda: any = null; // usamos any porque el backend devuelve campos extra como planNombre
  categorias: Categoria[] = [];
  destacados: ProductoPublic[] = [];
  loading = true;

  constructor(
    private tiendaService: TiendaService,
    private tiendaPublicService: TiendaPublicService,
    private categoriaService: CategoriaPublicService,
    private productoService: ProductoPublicService
  ) {}

  ngOnInit(): void {
    // Escuchar cambios de la tienda (cuando se cargue desde el servicio)
    this.tiendaService.currentTienda$.subscribe(tienda => {
      this.tienda = tienda;
      this.loading = false;
    });

    this.cargarDatosIniciales();
  }

  private cargarDatosIniciales(): void {
    // 1. CARGAR TIENDA (nombre, descripción, whatsapp, etc.)
    this.tiendaPublicService.cargarTiendaActual().subscribe({
      next: (tiendaResponse) => {
        this.tiendaService.setTienda(tiendaResponse); // ← esto dispara el currentTienda$
      },
      error: (err) => {
        console.error('Error cargando tienda:', err);
        this.loading = false;
      }
    });

    // 2. CATEGORÍAS
    this.categoriaService.getAll().subscribe({
      next: (cats) => this.categorias = cats,
      error: () => this.loading = false
    });

    // 3. PRODUCTOS DESTACADOS
    this.productoService.getAll().subscribe({
      next: (productos) => {
        this.destacados = productos
          .filter(p => p.stockTotal > 0)  // solo con stock
          .sort((a, b) => b.stockTotal - a.stockTotal) // más vendidos primero (aprox)
          .slice(0, 12);
      },
      error: () => this.loading = false
    });
  }
// Dentro de tu componente (por ejemplo, home.component.ts o donde esté esta sección)

getIconForCategory(nombre: string): string {
  const lower = nombre.toLowerCase();
  const iconMap: { [key: string]: string } = {
    // Personaliza según tus categorías reales
    'electrónica': 'bi-laptop',
    'computadora': 'bi-pc-display',
    'celular': 'bi-phone',
    'smartphone': 'bi-phone-vibrate',
    'audio': 'bi-headphones',
    'cámara': 'bi-camera',
    'televisor': 'bi-tv',
    'hogar': 'bi-house-door',
    'cocina': 'bi-egg-fried',
    'moda': 'bi-handbag',
    'ropa': 'bi-tshirt',
    'zapatos': 'bi-shoe',
    'belleza': 'bi-stars',
    'deporte': 'bi-trophy',
    'juguetes': 'bi-controller',
    'libros': 'bi-book',
    'música': 'bi-music-note-beamed',
    'gaming': 'bi-joystick',
    'accesorios': 'bi-gem',
    'salud': 'bi-heart-pulse',
    'jardín': 'bi-flower1',
    'automóvil': 'bi-car-front',
    'moto': 'bi-bicycle',
  };

  for (const key in iconMap) {
    if (lower.includes(key)) {
      return iconMap[key];
    }
  }
  return 'bi-bag-fill'; // fallback genérico
}

hasKnownIcon(nombre: string): boolean {
  const lower = nombre.toLowerCase();
  const keys = ['electrónica', 'computadora', 'celular', 'audio', 'cámara', 'televisor', 
                'hogar', 'cocina', 'moda', 'ropa', 'zapatos', 'belleza', 'deporte', 
                'juguetes', 'libros', 'música', 'gaming', 'accesorios', 'salud', 
                'jardín', 'automóvil', 'moto'];
  return keys.some(k => lower.includes(k));
}

getColorForCategory(index: number): string {
  const colors = [
    '#6366f1', // indigo
    '#8b5cf6', // violet
    '#ec4899', // pink
    '#f43f5e', // rose
    '#f97316', // orange
    '#10b981', // emerald
    '#14b8a6', // teal
    '#06b6d4', // cyan
    '#3b82f6', // blue
    '#a855f7', // purple
  ];
  return colors[index % colors.length];
}

}