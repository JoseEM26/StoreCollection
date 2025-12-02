// src/app/pages/publico/producto-unitario/producto-unitario.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPublic } from '../../../model/index.dto';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { TiendaService } from '../../../service/tienda.service';

interface Variante {
  precio: number;
  stock: number;
  imagenUrl: string;
  activo: boolean;
}

@Component({
  selector: 'app-producto-unitario',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './producto-unitario.component.html',
  styleUrls: ['./producto-unitario.component.css']
})
export class ProductoUnitarioComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productoService = inject(ProductoPublicService);
  private tiendaService = inject(TiendaService);

  producto!: ProductoPublic;
  tienda: any = null;
  loading = true;

  // Placeholder elegante
  fallbackImage = 'data:image/png;base64,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';

  varianteSeleccionada: Variante | null = null;
  imagenActual: string = '';

  // Lupa interna
  lupaVisible = false;
  lupaX = 0;
  lupaY = 0;

  // Modal
  showQuoteModal = false;
  clienteNombre = '';
  clienteTelefono = '';
  clienteMensaje = '';
  enviandoCotizacion = false;

  ngOnInit(): void {
    this.tiendaService.currentTienda$.subscribe(t => this.tienda = t);

    this.route.paramMap.subscribe(params => {
      const slug = params.get('productoSlug');
      if (slug) this.cargarProducto(slug);
    });
  }

  private cargarProducto(slug: string): void {
    this.loading = true;
    this.productoService.getBySlug(slug).subscribe({
      next: (prod) => {
        this.producto = prod;
        const varianteActiva = prod.variantes.find(v => v.activo && v.stock > 0) ||
                              prod.variantes.find(v => v.activo) || null;
        this.varianteSeleccionada = varianteActiva;
        this.actualizarImagen();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  seleccionarVariante(v: Variante) {
    if (v.activo && v.stock > 0) {
      this.varianteSeleccionada = v;
      this.actualizarImagen();
    }
  }

  cambiarImagen(url: string) {
    this.imagenActual = url || this.fallbackImage;
  }

  private actualizarImagen() {
    const url = this.varianteSeleccionada?.imagenUrl || this.producto.imagenPrincipal;
    this.imagenActual = url?.trim() ? url : this.fallbackImage;
  }

  // Getters
  get precioActual() { return this.varianteSeleccionada?.precio ?? this.producto.precioMinimo; }
  get hayStock() { return (this.varianteSeleccionada?.stock ?? 0) > 0; }
  get stockActual() { return this.varianteSeleccionada?.stock ?? 0; }
  get whatsappNumero() { return this.tienda?.whatsapp?.replace(/\D/g, '') || '51987654321'; }

  // Lupa interna
  mostrarLupa(e: MouseEvent) {
    this.lupaVisible = true;
    this.actualizarLupa(e);
  }

  ocultarLupa() {
    this.lupaVisible = false;
  }

  actualizarLupa(e: MouseEvent) {
    const img = e.currentTarget as HTMLElement;
    const rect = img.getBoundingClientRect();
    this.lupaX = e.clientX - rect.left;
    this.lupaY = e.clientY - rect.top;
  }

  // Acciones
  consultarWhatsApp() {
    const msg = encodeURIComponent(
      `¡Hola! Me interesa:\n\n*${this.producto.nombre}*\nS/ ${this.precioActual.toFixed(2)}\nStock: ${this.stockActual}\n\n¿Está disponible?`
    );
    window.open(`https://wa.me/${this.whatsappNumero}?text=${msg}`, '_blank');
  }

  llamarAhora() {
    window.location.href = `tel:${this.whatsappNumero}`;
  }

  abrirCotizacion() {
    this.showQuoteModal = true;
    this.clienteMensaje = `Hola, me interesa: ${this.producto.nombre}`;
  }

  cerrarModal() {
    this.showQuoteModal = false;
    this.clienteNombre = this.clienteTelefono = this.clienteMensaje = '';
  }

  enviarCotizacion() {
    if (!this.clienteNombre || !this.clienteTelefono) return;
    this.enviandoCotizacion = true;
    setTimeout(() => {
      alert(`¡Gracias ${this.clienteNombre}! Te contactamos pronto`);
      this.cerrarModal();
    }, 1500);
  }
}