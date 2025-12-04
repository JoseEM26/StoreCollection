// src/app/componente/public-layout/public-layout.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
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

  constructor(
    private tiendaService: TiendaService,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    this.tiendaService.currentTienda$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tienda => {
        this.tienda = tienda;
        if (tienda?.nombre) {
          this.titleService.setTitle(`${tienda.nombre} - Tienda Online`);
        }
      });
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu() {
    this.menuOpen = false;
  }

  abrirWhatsApp() {
    if (!this.tienda?.whatsapp) return;
    const numero = this.tienda.whatsapp.replace(/\D/g, '');
    const mensaje = encodeURIComponent(
      `¡Hola ${this.tienda.nombre}!\nVi tu tienda y quiero hacer un pedido\n\n¿Qué tienes disponible?`
    );
    window.open(`https://wa.me/${numero}?text=${mensaje}`, '_blank');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}