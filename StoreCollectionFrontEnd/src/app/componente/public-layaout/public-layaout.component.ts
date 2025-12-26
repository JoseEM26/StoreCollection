// src/app/componente/public-layout/public-layaout.component.ts

import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TiendaService } from '../../service/tienda.service';
import { CarritoService } from '../../service/carrito.service'; // ← NUEVA IMPORTACIÓN
import { Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './public-layaout.component.html',
  styleUrl: './public-layaout.component.css'
})
export class PublicLayaoutComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  tienda: any = null;
  menuOpen = false;
  currentYear = new Date().getFullYear();
  isScrolled = false;

  // Variables para el carrito en el header
  totalItemsCarrito = 0;

  constructor(
    private tiendaService: TiendaService,
    private carritoService: CarritoService, // ← NUEVO
    private titleService: Title
  ) {}

// En ngOnInit() del PublicLayoutComponent
ngOnInit(): void {
  // 1. Valor inmediato desde el service (gracias al resolver que ya lo cargó)
  this.tienda = this.tiendaService.currentTiendaValue;

  if (this.tienda?.nombre) {
    this.titleService.setTitle(`${this.tienda.nombre} - Tienda Online`);
  } else {
    this.titleService.setTitle('Tienda Online');
  }

  // 2. Suscripción para cambios futuros (por si se actualiza dinámicamente)
  this.tiendaService.currentTienda$
    .pipe(takeUntil(this.destroy$))
    .subscribe(tienda => {
      this.tienda = tienda;
      if (tienda?.nombre) {
        this.titleService.setTitle(`${tienda.nombre} - Tienda Online`);
      } else {
        this.titleService.setTitle('Tienda Online');
      }
    });

  // Carrito
  this.carritoService.itemsCount$
    .pipe(takeUntil(this.destroy$))
    .subscribe(count => {
      this.totalItemsCarrito = count;
    });

  this.onScroll();
}

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 20;
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  getWhatsAppLink(): string | null {
    if (!this.tienda?.whatsapp) return null;
    const numeroLimpio = this.tienda.whatsapp.replace(/\D/g, '');
    return `https://wa.me/${numeroLimpio}`;
  }

  abrirWhatsApp(): void {
    if (!this.tienda?.whatsapp) return;
    const numeroLimpio = this.tienda.whatsapp.replace(/\D/g, '');
    const mensaje = encodeURIComponent(
      `¡Hola ${this.tienda.nombre || 'de la tienda'}! 👋\nVi tu tienda online y estoy interesado/a en tus productos.\n\n¿Me puedes ayudar con más información o disponibilidad? 😊`
    );
    window.open(`https://wa.me/${numeroLimpio}?text=${mensaje}`, '_blank', 'noopener,noreferrer');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}