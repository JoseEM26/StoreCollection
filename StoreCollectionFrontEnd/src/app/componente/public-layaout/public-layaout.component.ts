import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,    // ← ESTO FALTABA (para routerLinkActive)
    RouterOutlet
  ],
  templateUrl: './public-layaout.component.html',
  styleUrl: './public-layaout.component.css'  
})
export class PublicLayaoutComponent {

  abrirWhatsApp() {
    const numero = '51987654321'; // ← Cambia por tu número real
    const mensaje = encodeURIComponent(
      `¡Hola! \nVi tu tienda y quiero hacer un pedido o consultar precios.\n\n¿Me ayudas?`
    );
    window.open(`https://wa.me/${numero}?text=${mensaje}`, '_blank');
  }
}