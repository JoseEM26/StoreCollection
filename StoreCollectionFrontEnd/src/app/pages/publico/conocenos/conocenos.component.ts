// conocenos.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TiendaPublicService } from '../../../service/tienda-public.service';
import { Tienda } from '../../../model';

@Component({
  selector: 'app-conocenos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conocenos.component.html',
  styleUrl: './conocenos.component.css'
})
export class ConocenosComponent implements OnInit {
  tienda!: Tienda;

  constructor(private tiendaPublicService: TiendaPublicService) {}

  ngOnInit(): void {
    this.tiendaPublicService.cargarTiendaActual().subscribe({
      next: (tienda) => {
        this.tienda = tienda;
      },
      error: (err) => {
        console.error('Error cargando tienda:', err);
      }
    });
  }

  // Helper para abrir WhatsApp
  abrirWhatsApp() {
    const mensaje = encodeURIComponent(`¡Hola ${this.tienda.nombre}! Quiero más información`);
    window.open(`https://wa.me/${this.tienda.whatsapp}?text=${mensaje}`, '_blank');
  }
}