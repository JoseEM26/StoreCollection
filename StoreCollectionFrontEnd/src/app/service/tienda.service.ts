// src/app/service/tienda.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Tienda } from '../model';

@Injectable({
  providedIn: 'root'
})
export class TiendaService {
  private slugSubject = new BehaviorSubject<string>('');
  public currentSlug$ = this.slugSubject.asObservable();

  private tiendaSubject = new BehaviorSubject<Tienda | null>(null);
  public currentTienda$ = this.tiendaSubject.asObservable();

  // Establece el slug actual (usado por el resolver)
  setSlug(slug: string): void {
    this.slugSubject.next(slug);
  }

  // Obtiene el slug actual de forma síncrona
  getCurrentSlug(): string {
    return this.slugSubject.value || '';
  }

  // Establece los datos completos de la tienda
  setTienda(tienda: Tienda | null): void {
    this.tiendaSubject.next(tienda);
  }

  // Limpia la tienda (útil al salir de una tienda o en errores)
  limpiarTienda(): void {
    this.tiendaSubject.next(null);
    this.slugSubject.next('');
  }

  // URL base para peticiones relacionadas con la tienda actual
  getBaseUrl(): string {
    const slug = this.getCurrentSlug();
    return slug ? `/api/public/tiendas/${slug}` : '';
  }

  // Valor síncrono actual (útil en guards o resolvers si necesitas valor inmediato)
  get currentTiendaValue(): Tienda | null {
    return this.tiendaSubject.value;
  }
}