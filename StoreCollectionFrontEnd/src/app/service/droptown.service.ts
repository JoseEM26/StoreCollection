import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment';
import { AtributoConValores, ProductoAdminListPage, VarianteResponse } from '../model/admin/producto-admin.model';
export interface DropTownStandar {
  id: number;
  descripcion: string;
}
@Injectable({
  providedIn: 'root'
})
export class DropTownService {

  private readonly API_URL = `${environment.apiUrl}/api/owner`; 

  constructor(private http: HttpClient) { }

  getTiendas(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(`${this.API_URL}/tiendasDropTown`);
  }
  getCategorias(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(`${this.API_URL}/categoriasDropTown`);
  }

  getUsuarios(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(`${this.API_URL}/usuariosDropTown`);
  }

  getPlanes(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(`${this.API_URL}/planesDropTown`);
  }
 

 getAtributosConValores(): Observable<AtributoConValores[]> {
  return this.http.get<AtributoConValores[]>(`${this.API_URL}/atributosDropTown`);
  }

// === NUEVOS PARA VENTA DIRECTA ===

  /**
   * Obtiene productos con stock disponible para venta directa
   * Filtrado por nombre y paginado
   */
getProductosConStock(
  nombre?: string,
  page: number = 0,
  size: number = 20,
  productoIdConVariantes?: number  // ← este es clave
): Observable<ProductoAdminListPage> {
  let params = new HttpParams()
    .set('page', page.toString())
    .set('size', size.toString());

  if (nombre?.trim()) {
    params = params.set('nombre', nombre.trim());
  }

  // ¡IMPORTANTÍSIMO! Solo agregar si viene definido
  if (productoIdConVariantes !== undefined && productoIdConVariantes !== null) {
    params = params.set('productoIdConVariantes', productoIdConVariantes.toString());
  }

  return this.http.get<ProductoAdminListPage>(
    `${this.API_URL}/productos-con-stock`,
    { params }
  );
}

}