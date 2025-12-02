// src/app/pages/publico/producto-unitario/producto-unitario.component.ts
import { Component, OnInit, AfterViewInit, inject, ElementRef } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoPublicService } from '../../../service/producto-public.service';
import { TiendaService } from '../../../service/tienda.service';
import { ProductoPublic } from '../../../model/index.dto';

@Component({
  selector: 'app-producto-unitario',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './producto-unitario.component.html',
  styleUrls: ['./producto-unitario.component.css']
})
export class ProductoUnitarioComponent implements OnInit, AfterViewInit {
  private route = inject(ActivatedRoute);
  private productoService = inject(ProductoPublicService);
  private tiendaService = inject(TiendaService);
  private el = inject(ElementRef);

  producto!: ProductoPublic;
  tienda: any = null;
  loading = true;

  // Modal cotización
  showQuoteModal = false;
  clienteNombre = '';
  clienteTelefono = '';
  clienteMensaje = '';
  enviandoCotizacion = false;

  ngOnInit(): void {
    this.tiendaService.currentTienda$.subscribe(t => this.tienda = t);

    this.route.paramMap.subscribe(params => {
      const slug = params.get('productoSlug');
      if (slug) {
        this.cargarProducto(slug);
      }
    });
  }

  private cargarProducto(slug: string): void {
    this.loading = true;
    this.productoService.getBySlug(slug).subscribe({
      next: (prod) => {
        this.producto = prod;
        this.loading = false;
        console.log('Producto cargado:', prod); // ← para que veas qué llega
      },
      error: (err) => {
        console.error('Error al cargar producto:', err);
        this.loading = false;
      }
    });
  }

  // === CORREGIDO: usar los campos correctos del DTO ===
  get precio(): number {
    return this.producto?.precioMinimo || 0;
  }

  get hayStock(): boolean {
    return (this.producto?.stockTotal || 0) > 0;
  }

  get stockActual(): number {
    return this.producto?.stockTotal || 0;
  }

  get imagenUrl(): string {
    return this.producto?.imagenPrincipal || 'https://placehold.co/800x800/eeeeee/999999.png?text=Sin+Imagen';
  }

  get imagenZoom(): string {
    return this.producto?.imagenPrincipal || 'https://placehold.co/1600x1600/eeeeee/999999.png?text=Zoom';
  }

  // === WhatsApp con número real de la tienda ===
  get whatsappNumero(): string {
    if (!this.tienda?.whatsapp) return '51987654321';
    return this.tienda.whatsapp.replace(/\D/g, ''); // quita espacios, guiones, etc.
  }

  consultarWhatsApp() {
    const msg = encodeURIComponent(
      `¡Hola! 👋\n\nQuiero este producto:\n\n` +
      `Nombre: *${this.producto.nombre}*\n` +
      `Precio: S/ ${this.precio.toFixed(2)}\n` +
      `Categoría: ${this.producto.nombreCategoria}\n\n` +
      `¿Está disponible? ¿Hacen envíos?\n¡Gracias!`
    );
    window.open(`https://wa.me/${this.whatsappNumero}?text=${msg}`, '_blank');
  }

  llamarAhora() {
    window.location.href = `tel:${this.whatsappNumero}`;
  }

  abrirCotizacion() {
    this.showQuoteModal = true;
    this.clienteMensaje = `Hola, me interesa: ${this.producto.nombre} - S/ ${this.precio.toFixed(2)}`;
  }

  cerrarModal() {
    this.showQuoteModal = false;
    this.clienteNombre = '';
    this.clienteTelefono = '';
    this.clienteMensaje = '';
    this.enviandoCotizacion = false;
  }

  enviarCotizacion() {
    if (!this.clienteNombre.trim() || !this.clienteTelefono.trim()) return;
    this.enviandoCotizacion = true;
    setTimeout(() => {
      alert(`¡Gracias ${this.clienteNombre}! Te contactamos pronto.`);
      this.cerrarModal();
    }, 1500);
  }

  // === ZOOM DE IMAGEN (funciona perfecto) ===
  ngAfterViewInit(): void {
    const container = this.el.nativeElement.querySelector('.imagen-zoom-container');
    const lupa = this.el.nativeElement.querySelector('.lupa');
    const imgLupa = this.el.nativeElement.querySelector('.img-lupa');

    if (!container || !lupa || !imgLupa) return;

    const mover = (e: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;

      lupa.classList.add('active');
      lupa.style.left = x + 'px';
      lupa.style.top = y + 'px';

      const px = (x / rect.width) * 100;
      const py = (y / rect.height) * 100;
      imgLupa.style.transform = `translate(-${px}%, -${py}%) scale(2.5)`;
    };

    container.addEventListener('mousemove', mover);
    container.addEventListener('mouseenter', () => lupa.classList.add('visible'));
    container.addEventListener('mouseleave', () => lupa.classList.remove('active', 'visible'));
  }
}