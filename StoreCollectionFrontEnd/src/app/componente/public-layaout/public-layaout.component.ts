// src/app/componente/public-layout/public-layout.component.ts
import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TiendaService } from '../../service/tienda.service';
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

  constructor(
    private tiendaService: TiendaService,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    // Suscripción a la tienda actual
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

    // Detectar scroll para efecto en header
    this.onScroll(); // Estado inicial
  }

  // Mejor práctica: usar @HostListener para eventos de window
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

  // MÉTODO PARA GENERAR EL LINK DE WHATSAPP (seguro y limpio)
  getWhatsAppLink(): string | null {
    if (!this.tienda?.whatsapp) return null;

    // Limpia todo lo que no sea número (espacios, +, -, (), etc.)
    const numeroLimpio = this.tienda.whatsapp.replace(/\D/g, '');

    // Opcional: asegura que tenga código de país (Perú: 51)
    // Si no empieza con 51 y tiene 9 dígitos, agrega 51
    // if (numeroLimpio.length === 9) {
    //   return `https://wa.me/51${numeroLimpio}`;
    // }

    return `https://wa.me/${numeroLimpio}`;
  }

  // MÉTODO PARA ABRIR WHATSAPP CON MENSAJE PREDETERMINADO
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
    // No es necesario remover el listener si usas @HostListener
  }
}