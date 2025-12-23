import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environment';
import { PlanPage } from '../model/admin/plan-admin.model';

@Injectable({
  providedIn: 'root'
})
export class PlanPublicService {
  private apiUrl = `${environment.apiUrl}/api/public/planes`;

  constructor(private http: HttpClient) {}

  listarVigentes(page: number = 0, size: number = 10): Observable<PlanPage> {
    return this.http.get<PlanPage>(this.apiUrl, {
      params: { page: page.toString(), size: size.toString() }
    });
  }
}