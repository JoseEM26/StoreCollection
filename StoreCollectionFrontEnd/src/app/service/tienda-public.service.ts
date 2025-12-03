// src/app/services/tienda-public.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { TiendaService } from './tienda.service';
import { Observable } from 'rxjs';
import { Tienda } from '../model';
import { tap } from 'rxjs/operators';
import { TiendaPage } from '../model/tienda-public.model';
import { environment } from '../../../environment';

@Injectable({ providedIn: 'root' })
export class TiendaPublicService {
  constructor(private http: HttpClient, private tiendaService: TiendaService) {}
 private apiUrl = `${environment.apiUrl}/api/public/tiendas`;
  getAllTiendas(
    page = 0,
    size = 12,
    sort = 'nombre,asc',
    search?: string
  ): Observable<TiendaPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<TiendaPage>(this.apiUrl, { params });
  }
  cargarTiendaActual(): Observable<Tienda> {
    const url = this.tiendaService.getBaseUrl();
    return this.http
      .get<Tienda>(url)
      .pipe(tap((tienda) => this.tiendaService.setTienda(tienda)));
  }
}
