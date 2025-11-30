// src/app/app.routes.ts
import { Routes } from '@angular/router';

// Páginas públicas
import { MainTiendaComponent } from './pages/publico/main-tienda/main-tienda.component';
import { CatalogoComponent } from './pages/publico/catalogo/catalogo.component';
import { ProductoUnitarioComponent } from './pages/publico/producto-unitario/producto-unitario.component';

// Admin
import { LoginComponent } from './pages/administrativo/login/login.component';
import { AdminLayoutComponent } from './componente/admin-layout.component/admin-layout.component';
import { authGuard } from '../auth/auth.guard';
import { DashboardComponent } from './pages/administrativo/dashboard/dashboard.component';
import { StoresComponent } from './pages/administrativo/stores.component/stores.component';
import { CategoriesComponent } from './pages/administrativo/categories.component/categories.component';
import { ProductsComponent } from './pages/administrativo/products.component/products.component';
import { PublicLayaoutComponent } from './componente/public-layaout/public-layaout.component';

export const routes: Routes = [

  // === PÚBLICO CON LAYOUT (header + botón WhatsApp flotante) ===
  {
    path: '',
    component: PublicLayaoutComponent,
    children: [
      { path: '', component: MainTiendaComponent },
      { path: 'catalogo', component: CatalogoComponent },
      { path: 'catalogo/:categoria', component: CatalogoComponent },
      { path: 'producto/:id', component: ProductoUnitarioComponent },
    ]
  },

  // === LOGIN Y ADMIN ===
  { path: 'login', component: LoginComponent },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'stores', component: StoresComponent },
      { path: 'categories', component: CategoriesComponent },
      { path: 'products', component: ProductsComponent },
    ]
  },

  { path: '**', redirectTo: '' }
];