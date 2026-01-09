import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'StoreCollectionFrontEnd';

  constructor(private router: Router) {}

  ngOnInit() {
    // Escuchamos cada cambio de ruta completado
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      // Pequeño timeout para que funcione bien en móviles y navegadores "rebeldes"
      setTimeout(() => {
        window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
        // Estas dos líneas ayudan especialmente en Safari/iOS
        document.body.scrollTop = 0;
        document.documentElement.scrollTop = 0;
      }, 0); // 0 suele bastar, puedes probar 10 o 50 si sigue fallando en algún caso
    });
  }
}