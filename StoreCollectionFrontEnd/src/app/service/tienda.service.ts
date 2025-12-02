// src/app/services/tienda.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Tienda } from '../model';

@Injectable({ providedIn: 'root' })
export class TiendaService {

  private slugSubject = new BehaviorSubject<string>('');
  currentSlug$ = this.slugSubject.asObservable();

  // Aquí guardamos los datos reales de la tienda una vez cargados
  private tiendaSubject = new BehaviorSubject<Tienda | null>(null);
  currentTienda$ = this.tiendaSubject.asObservable();

  setSlug(slug: string) {
    this.slugSubject.next(slug);
  }

  getBaseUrl(): string {
    const slug = this.slugSubject.value;
    if (!slug) {
      console.error('Slug de tienda no establecido');
      return '/api/public/tiendas/error';
    }
    return `/api/public/tiendas/${slug}`;
  }

  // Método interno usado por TiendaPublicService
  setTienda(tienda: Tienda) {
    this.tiendaSubject.next(tienda);
  }
}