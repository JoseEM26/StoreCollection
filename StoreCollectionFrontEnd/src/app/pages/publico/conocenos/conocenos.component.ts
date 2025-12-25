// src/app/pages/publico/conocenos/conocenos.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TiendaService } from '../../../service/tienda.service';
import { Tienda } from '../../../model';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-conocenos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conocenos.component.html',
  styleUrl: './conocenos.component.css'
})
export class ConocenosComponent implements OnInit, OnDestroy {
  tienda: Tienda | null = null;
  private destroy$ = new Subject<void>();

  constructor(private tiendaService: TiendaService) {}

  ngOnInit(): void {
    // Valor inmediato (gracias al TiendaResolver)
    this.tienda = this.tiendaService.currentTiendaValue;

    // Suscripción para cualquier cambio futuro (por si se actualiza dinámicamente)
    this.tiendaService.currentTienda$
      .pipe(takeUntil(this.destroy$))
      .subscribe(tienda => {
        this.tienda = tienda;
      });
  }

  abrirWhatsApp(): void {
    if (!this.tienda?.whatsapp || !this.tienda?.nombre) {
      console.warn('WhatsApp no configurado para esta tienda');
      return;
    }

    const numeroLimpio = this.tienda.whatsapp.replace(/\D/g, '');
    const mensaje = encodeURIComponent(
      `¡Hola ${this.tienda.nombre}! 👋\nEstoy visitando tu página "Conócenos" y me gustaría más información sobre tus productos y servicios. ¿Me puedes ayudar? 😊`
    );

    window.open(
      `https://wa.me/${numeroLimpio}?text=${mensaje}`,
      '_blank',
      'noopener,noreferrer'
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}