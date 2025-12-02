// src/app/componente/public-layout/public-layout.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TiendaService } from '../../service/tienda.service';
import { Title } from '@angular/platform-browser';
import { CommonModule, NgIf } from '@angular/common';


@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet
],
  providers: [Title,CommonModule], 
  templateUrl: './public-layaout.component.html',
  styleUrl: './public-layaout.component.css'  
})
export class PublicLayaoutComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  tienda: any = null;

  constructor(
    private tiendaService: TiendaService,
    private titleService: Title   // ya funciona
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

  abrirWhatsApp() {
    if (!this.tienda?.whatsapp) return;
    const numero = this.tienda.whatsapp.replace(/\D/g, '');
    const mensaje = encodeURIComponent(
      `¡Hola! %0AVi tu tienda *${this.tienda.nombre}* y quiero hacer un pedido 🛒%0A%0A¿Me ayudas?`
    );
    window.open(`https://wa.me/${numero}?text=${mensaje}`, '_blank');
  }

  get currentYear(): number {
    return new Date().getFullYear();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}