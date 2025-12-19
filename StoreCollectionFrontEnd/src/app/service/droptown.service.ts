import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment';
export interface DropTownStandar {
  id: number;
  descripcion: string;
}
@Injectable({
  providedIn: 'root'
})
export class DropTownService {

  private readonly API_URL = `${environment.apiUrl}/api/owner`;  // ← Usamos environment

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

  getAtributos(): Observable<DropTownStandar[]> {
    return this.http.get<DropTownStandar[]>(`${this.API_URL}/atributosDropTown`);
  }
}