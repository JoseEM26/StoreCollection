// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { MainTiendaComponent } from './pages/publico/main-tienda/main-tienda.component';
import { CatalogoComponent } from './pages/publico/catalogo/catalogo.component';
import { ProductoUnitarioComponent } from './pages/publico/producto-unitario/producto-unitario.component';

export const routes: Routes = [
  { path: '', component: MainTiendaComponent },
  { path: 'catalogo', component: CatalogoComponent },
  { path: 'catalogo/:categoria', component: CatalogoComponent },
  { path: 'producto/:id', component: ProductoUnitarioComponent },
  { path: '**', redirectTo: '' }
];