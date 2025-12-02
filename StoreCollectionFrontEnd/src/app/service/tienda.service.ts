// src/app/services/tienda.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Tienda } from '../model';

@Injectable({ providedIn: 'root' })
export class TiendaService {
  private slugSubject = new BehaviorSubject<string>('');
  currentSlug$ = this.slugSubject.asObservable();

  private tiendaSubject = new BehaviorSubject<Tienda | null>(null);
  currentTienda$ = this.tiendaSubject.asObservable();

  setSlug(slug: string) {
    this.slugSubject.next(slug);
  }

  getCurrentSlug(): string {
    return this.slugSubject.value || '';
  }

  getBaseUrl(): string {
    const slug = this.getCurrentSlug();
    if (!slug) return '';
    return `/api/public/tiendas/${slug}`;
  }

  setTienda(tienda: Tienda) {
    this.tiendaSubject.next(tienda);
  }

  getTienda(): Tienda | null {
    return this.tiendaSubject.value;
  }
}