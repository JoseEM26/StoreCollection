// src/app/services/tienda.resolver.ts
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { TiendaService } from './tienda.service';

@Injectable({ providedIn: 'root' })
export class TiendaResolver implements Resolve<string> {
  constructor(private tiendaService: TiendaService) {}

  resolve(route: ActivatedRouteSnapshot): string {
    const slug = route.paramMap.get('tiendaSlug')!;
    this.tiendaService.setSlug(slug);  
    return slug;
  }
}