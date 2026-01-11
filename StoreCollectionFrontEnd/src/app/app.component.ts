import { Component, OnInit, Renderer2, Inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RouterOutlet } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { TiendaService } from './service/tienda.service'; // ajusta la ruta según tu estructura
import { Tienda } from './model'; // ajusta la ruta

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Store Collection';

  constructor(
    private router: Router,
    private tiendaService: TiendaService,
    private renderer: Renderer2,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit() {
    // 1. Scroll al inicio en cada cambio de ruta
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      setTimeout(() => {
        window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
        document.body.scrollTop = 0;
        document.documentElement.scrollTop = 0;
      }, 0);
    });

    // 2. Escuchar cambios en la tienda actual → actualizar favicon + título
    this.tiendaService.currentTienda$.subscribe(tienda => {
      if (tienda) {
        this.updateFaviconAndTitle(tienda);
      } else {
        this.restoreDefaultFaviconAndTitle();
      }
    });
  }

  private updateFaviconAndTitle(tienda: Tienda) {
    // Actualizar título
    const tiendaName = tienda.nombre?.trim() || 'Tienda';
    this.document.title = `${tiendaName} • Store Collection`;

    // Limpiar favicons anteriores
    this.removeExistingFavicons();

    // Si la tienda tiene logo → usarlo como favicon
    if (tienda.logo_img_url) {
      const link = this.document.createElement('link');
      link.rel = 'icon';
      link.type = 'image/png';           // ← cambia a image/jpeg si es jpg
      link.href = tienda.logo_img_url;

      // Opcional: agregar también apple-touch-icon (muy recomendado en móviles)
      const appleLink = this.document.createElement('link');
      appleLink.rel = 'apple-touch-icon';
      appleLink.href = tienda.logo_img_url; // idealmente 180x180

      this.renderer.appendChild(this.document.head, link);
      this.renderer.appendChild(this.document.head, appleLink);
    }
  }

  private restoreDefaultFaviconAndTitle() {
    this.document.title = 'Store Collection – Panel Administrativo';

    this.removeExistingFavicons();

    const defaultLink = this.document.createElement('link');
    defaultLink.rel = 'icon';
    defaultLink.type = 'image/x-icon';
    defaultLink.href = 'https://res.cloudinary.com/dqznlmig0/image/upload/v1767996009/PineTools.com_imagen_2026-01-09_165850754-removebg-preview_a0dxex.png';

    this.renderer.appendChild(this.document.head, defaultLink);
  }

  private removeExistingFavicons() {
    const selectors = [
      'link[rel="icon"]',
      'link[rel="shortcut icon"]',
      'link[rel="apple-touch-icon"]'
    ];

    selectors.forEach(selector => {
      const links = this.document.querySelectorAll(selector);
      links.forEach(link => link.remove());
    });
  }
}