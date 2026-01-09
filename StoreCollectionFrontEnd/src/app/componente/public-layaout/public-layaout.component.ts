import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TiendaService } from '../../service/tienda.service';
import { CarritoService } from '../../service/carrito.service';
import { Title } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './public-layaout.component.html',
  styleUrl: './public-layaout.component.css'
})
export class PublicLayaoutComponent implements OnInit, OnDestroy {   // ← nombre de clase corregido
  private destroy$ = new Subject<void>();

  tienda: any = null;
  menuOpen = false;
  currentYear = new Date().getFullYear();
  isScrolled = false;
instagramUrl: string | null = null;
  tiktokUrl: string | null = null;
  facebookUrl: string | null = null;
  totalItemsCarrito = 0;

  socialLinks: { platform: string; url: string; icon: string }[] = [];

  constructor(
    private tiendaService: TiendaService,
    private carritoService: CarritoService,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    this.tienda = this.tiendaService.currentTiendaValue;
    this.updateTitle();
    this.updateSocialLinks();

    // Suscripciones
    this.tiendaService.currentTienda$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tienda => {
        this.tienda = tienda;
        this.updateTitle();
        this.updateSocialLinks();
      });

    this.carritoService.itemsCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.totalItemsCarrito = count || 0;
      });

    this.onScroll();
  }

  private updateTitle(): void {
    const nombre = this.tienda?.nombre?.trim();
    this.titleService.setTitle(nombre ? `${nombre} - Tienda Online` : 'Tienda Online');
  }

  private normalizeUrl(platform: string, value: string | undefined): string | null {
    if (!value?.trim()) return null;

    const url = value.trim();

    // Si ya es URL completa válida, la retornamos
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }

    // Limpiamos @ y armamos según plataforma
    const username = url.replace(/^@/, '').trim();

    const bases = {
      facebook: 'https://www.facebook.com/',
      instagram: 'https://www.instagram.com/',
      tiktok: 'https://www.tiktok.com/@'
    };

    return bases[platform as keyof typeof bases] + username;
  }

private updateSocialLinks(): void {
    this.socialLinks = [];
    this.instagramUrl = null;
    this.tiktokUrl = null;
    this.facebookUrl = null;

    if (!this.tienda) return;

    // Facebook
    if (this.tienda.facebook?.trim()) {
      let url = this.tienda.facebook.trim();
      if (!url.startsWith('http')) {
        url = 'https://www.facebook.com/' + url.replace(/^@/, '');
      }
      this.socialLinks.push({ platform: 'Facebook', url, icon: 'bi-facebook' });
      this.facebookUrl = url;
    }

    // Instagram
    if (this.tienda.instagram?.trim()) {
      let url = this.tienda.instagram.trim();
      if (!url.startsWith('http')) {
        url = 'https://www.instagram.com/' + url.replace(/^@/, '');
      }
      this.socialLinks.push({ platform: 'Instagram', url, icon: 'bi-instagram' });
      this.instagramUrl = url;
    }

    // TikTok (mejor manejo cuando ya viene con URL completa)
    if (this.tienda.tiktok?.trim()) {
      let url = this.tienda.tiktok.trim();
      // Si ya es URL completa, no tocamos
      if (!url.startsWith('http')) {
        const username = url.replace(/^@/, '').trim();
        url = 'https://www.tiktok.com/@' + username;
      }
      this.socialLinks.push({ platform: 'TikTok', url, icon: 'bi-tiktok' });
      this.tiktokUrl = url;
    }
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
    const numero = this.tienda.whatsapp.replace(/\D/g, '');
    return numero.length >= 9 ? `https://wa.me/${numero}` : null;
  }

  abrirWhatsApp(): void {
    const link = this.getWhatsAppLink();
    if (!link) return;

    const nombre = this.tienda?.nombre?.trim() || 'la tienda';
    const mensaje = encodeURIComponent(
      `¡Hola! 😊\n` +
      `Vi tu tienda ${nombre} y me interesaron tus productos.\n\n` +
      `¿Me puedes dar más información por favor? 🙌`
    );

    window.open(`${link}?text=${mensaje}`, '_blank', 'noopener,noreferrer');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}