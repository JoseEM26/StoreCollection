// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
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
@ViewChild('categoriesScroller') categoriesScroller!: ElementRef;

showLeftArrow = false;
showRightArrow = true;
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


ngAfterViewInit(): void {
  this.checkArrows(); // Inicial
}

onScroll(event: any): void {
  this.checkArrows();
}

scrollCategories(direction: 'left' | 'right'): void {
  const scroller = this.categoriesScroller.nativeElement;
  const cardWidth = scroller.querySelector('.category-card')?.offsetWidth || 160;
  const scrollAmount = cardWidth * 3 + 48; // 3 tarjetas + gap

  if (direction === 'left') {
    scroller.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
  } else {
    scroller.scrollBy({ left: scrollAmount, behavior: 'smooth' });
  }
}

private checkArrows(): void {
  const scroller = this.categoriesScroller.nativeElement;
  const tolerance = 10; // px de tolerancia

  this.showLeftArrow = scroller.scrollLeft > tolerance;
  this.showRightArrow = (scroller.scrollLeft + scroller.clientWidth) < (scroller.scrollWidth - tolerance);
}

getColorForCategory(index: number): string {
  const colors = [
    '#10b981', // emerald
    '#14b8a6', // teal
    '#626262', // purple
  ];
  return colors[index % colors.length];
}

}