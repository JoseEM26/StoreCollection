// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { Categoria, Producto } from '../../../model';
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

  tienda: any = null;  // ← usamos any temporalmente para que funcione con TiendaResponse
  categorias: Categoria[] = [];
  destacados: Producto[] = [];
  loading = true;

  constructor(
    private tiendaService: TiendaService,
    private tiendaPublicService: TiendaPublicService,
    private categoriaService: CategoriaPublicService,
    private productoService: ProductoPublicService
  ) {}

  ngOnInit(): void {
    // Escuchar cuando llegue la tienda real
    this.tiendaService.currentTienda$.subscribe(tienda => {
      this.tienda = tienda;
      console.log('Tienda cargada:', tienda); // ← para debug
    });

    this.cargarDatos();
  }

  private cargarDatos(): void {
    // 1. Cargar info de la tienda (nombre, whatsapp, etc.)
    this.tiendaPublicService.cargarTiendaActual().subscribe({
      next: (tiendaResponse) => {
        this.tiendaService.setTienda(tiendaResponse); // ← ¡¡IMPORTANTE!!
      },
      error: (err) => {
        console.error('Error cargando tienda:', err);
        this.loading = false;
      }
    });

    // 2. Categorías
    this.categoriaService.getAll().subscribe({
      next: (cats) => this.categorias = cats,
      error: () => this.loading = false
    });

    // 3. Productos destacados
    this.productoService.getAll().subscribe({
      next: (productos) => {
        this.destacados = productos.slice(0, 8);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}