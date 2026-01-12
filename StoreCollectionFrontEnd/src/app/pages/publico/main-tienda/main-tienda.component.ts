// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component, ElementRef, OnInit, ViewChild, AfterViewInit } from '@angular/core';
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
export class MainTiendaComponent implements OnInit, AfterViewInit {
  tienda: any = null;
  categorias: Categoria[] = [];
  destacados: ProductoPublic[] = [];
  loading = true;

  @ViewChild('categoriesScroller') categoriesScroller?: ElementRef;

  showLeftArrow = false;
  showRightArrow = true;

  constructor(
    private tiendaService: TiendaService,
    private tiendaPublicService: TiendaPublicService,
    private categoriaService: CategoriaPublicService,
    private productoService: ProductoPublicService
  ) {}

  ngOnInit(): void {
    // Escuchar cambios de la tienda
    this.tiendaService.currentTienda$.subscribe(tienda => {
      this.tienda = tienda;
      this.loading = false;
    });

    this.cargarDatosIniciales();
  }

  ngAfterViewInit(): void {
    // Damos un pequeño tiempo para que el *ngIf se evalúe
    setTimeout(() => {
      this.updateArrowsVisibility();
    }, 0);
  }

  private cargarDatosIniciales(): void {
    // 1. CARGAR TIENDA
    this.tiendaPublicService.cargarTiendaActual().subscribe({
      next: (tiendaResponse) => {
        this.tiendaService.setTienda(tiendaResponse);
      },
      error: (err) => {
        console.error('Error cargando tienda:', err);
        this.loading = false;
      }
    });

    // 2. CATEGORÍAS
    this.categoriaService.getAll().subscribe({
      next: (cats) => {
        this.categorias = cats;
        // Actualizamos flechas después de cargar categorías
        setTimeout(() => this.updateArrowsVisibility(), 100);
      },
      error: () => this.loading = false
    });

    // 3. PRODUCTOS DESTACADOS
    this.productoService.getAll().subscribe({
      next: (productos) => {
        this.destacados = productos
          .filter(p => p.stockTotal > 0)
          .sort((a, b) => b.stockTotal - a.stockTotal)
          .slice(0, 12);
      },
      error: () => this.loading = false
    });
  }

  onScroll(event: any): void {
    this.updateArrowsVisibility();
  }

  scrollCategories(direction: 'left' | 'right'): void {
    if (!this.categoriesScroller?.nativeElement) return;

    const scroller = this.categoriesScroller.nativeElement;
    const cardWidth = scroller.querySelector('.category-card')?.offsetWidth || 160;
    const scrollAmount = cardWidth * 3 + 48; // 3 tarjetas + gap aproximado

    if (direction === 'left') {
      scroller.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    } else {
      scroller.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }

    // Actualizamos visibilidad después del scroll
    setTimeout(() => this.updateArrowsVisibility(), 300);
  }

  private updateArrowsVisibility(): void {
    if (!this.categoriesScroller?.nativeElement) {
      this.showLeftArrow = false;
      this.showRightArrow = false;
      return;
    }

    const el = this.categoriesScroller.nativeElement;
    const tolerance = 10; // px

    this.showLeftArrow = el.scrollLeft > tolerance;

    // Más precisa: hay más contenido a la derecha si el scroll restante es significativo
    this.showRightArrow = 
      Math.ceil(el.scrollLeft + el.clientWidth) < el.scrollWidth - tolerance;
  }

  getColorForCategory(index: number): string {
    const colors = [
      '#10b981', // emerald
      '#14b8a6', // teal
      '#8b5cf6', // violet (cambié el gris por algo más vivo)
    ];
    return colors[index % colors.length];
  }
}