// src/app/pages/publico/producto-unitario/producto-unitario.component.ts
import { Component, inject, AfterViewInit, Renderer2, ElementRef, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductosService } from '../../../service/productos.service';
import { Producto } from '../../../model/producto.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-producto-unitario',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './producto-unitario.component.html',
  styleUrls: ['./producto-unitario.component.css']
})
export class ProductoUnitarioComponent implements AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private service = inject(ProductosService);
  private renderer = inject(Renderer2);
  private el = inject(ElementRef);

  producto: Producto | undefined;
  private listeners: (() => void)[] = [];

  // WhatsApp del negocio (cámbialo por el tuyo)
  whatsappNumber = '51987654321'; // ← Cambia aquí tu número real

  // Formulario de cotización
  showQuoteModal = false;
  clienteNombre = '';
  clienteTelefono = '';
  clienteMensaje = '';
  enviandoCotizacion = false;

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.producto = this.service.getById(id);
  }

  get hayStock(): boolean {
    return this.producto ? this.producto.stock > 0 : false;
  }

  // WhatsApp directo con producto
  consultarWhatsApp() {
    if (!this.producto) return;

    const mensaje = encodeURIComponent(
      `¡Hola! 👋\n\nEstoy interesado en este producto:\n\n` +
      `📦 *${this.producto.nombre}*\n` +
      `💰 Precio: $${this.producto.precio}\n` +
      `📄 ${this.producto.descripcion}\n\n` +
      `¿Está disponible? ¿Tienen envío a mi zona? ¿Cuánto sería el total?\n\n¡Gracias!`
    );

    window.open(`https://wa.me/${this.whatsappNumber}?text=${mensaje}`, '_blank');
  }

  // Llamar directamente
  llamarAhora() {
    window.location.href = `tel:${this.whatsappNumber}`;
  }

  // Abrir modal de cotización
  abrirCotizacion() {
    this.showQuoteModal = true;
    this.clienteMensaje = `Hola, me interesa el producto: ${this.producto?.nombre} - $${this.producto?.precio}`;
  }

  cerrarModal() {
    this.showQuoteModal = false;
    this.enviandoCotizacion = false;
  }

  enviarCotizacion() {
    if (!this.clienteNombre || !this.clienteTelefono) return;

    this.enviandoCotizacion = true;

    // Aquí conectarás después con tu API de correos
    setTimeout(() => {
      alert(`¡Cotización enviada!\n\nPronto te contactaremos al ${this.clienteTelefono}`);
      this.cerrarModal();
      this.clienteNombre = '';
      this.clienteTelefono = '';
      this.clienteMensaje = '';
    }, 1500);
  }

 ngAfterViewInit(): void {
  const container = this.el.nativeElement.querySelector('.imagen-zoom-container');
  const lupa = this.el.nativeElement.querySelector('.lupa');
  const imgLupa = this.el.nativeElement.querySelector('.img-lupa');

  if (!container || !lupa || !imgLupa) return;

  container.addEventListener('mousemove', (e: MouseEvent) => {
    const rect = container.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    lupa.style.left = x + 'px';
    lupa.style.top = y + 'px';
    lupa.classList.add('active');

    const posX = (x / rect.width) * 100;
    const posY = (y / rect.height) * 100;
    imgLupa.style.left = `${-posX * 2}%`;
    imgLupa.style.top = `${-posY * 2}%`;
  });

  container.addEventListener('mouseleave', () => {
    lupa.classList.remove('active');
  });
}

  ngOnDestroy(): void {
    this.listeners.forEach(unlisten => unlisten());
  }
}