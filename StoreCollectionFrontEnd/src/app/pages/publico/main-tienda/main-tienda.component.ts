// src/app/pages/publico/main-tienda/main-tienda.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductoCardComponent } from '../../../componente/producto-card/producto-card.component';
import { Categoria, Producto, Tienda } from '../../../model';
import { TiendaService } from '../../../service/tienda.service';
import { TiendaPublicService } from '../../../service/tienda-public.service';
import { CategoriaPublicService } from '../../../service/categoria-public.service';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { ProductoPublic } from '../../../model/index.dto';

@Component({
  selector: 'app-main-tienda',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductoCardComponent],
  templateUrl: './main-tienda.component.html',
  styleUrls: ['./main-tienda.component.css']
})
export class MainTiendaComponent implements OnInit {
  tienda: Tienda | null = null;
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
    this.tiendaService.currentTienda$.subscribe(t => this.tienda = t);
    this.cargarDatos();
  }

  private cargarDatos(): void {
    this.tiendaPublicService.cargarTiendaActual().subscribe({
      error: () => this.loading = false
    });

    this.categoriaService.getAll().subscribe({
      next: (cats) => this.categorias = cats,
      error: () => this.loading = false
    });

    this.productoService.getAll().subscribe({
  next: (productos) => {
    this.destacados = productos.slice(0, 12);
    this.loading = false;
  }
});
  }
}