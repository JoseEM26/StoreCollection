// src/app/app.routes.ts
import { Routes } from '@angular/router';

// === PÁGINAS PÚBLICAS ===
import { MainTiendaComponent } from './pages/publico/main-tienda/main-tienda.component';
import { CatalogoComponent } from './pages/publico/catalogo/catalogo.component';
import { ProductoUnitarioComponent } from './pages/publico/producto-unitario/producto-unitario.component';
import { PublicLayaoutComponent } from './componente/public-layaout/public-layaout.component';

// === ADMIN ===
import { LoginComponent } from './pages/administrativo/login/login.component';
import { AdminLayoutComponent } from './componente/admin-layout.component/admin-layout.component';
import { authGuard } from '../auth/auth.guard';
import { DashboardComponent } from './pages/administrativo/dashboard/dashboard.component';
import { StoresComponent } from './pages/administrativo/stores.component/stores.component';
import { CategoriesComponent } from './pages/administrativo/categories.component/categories.component';
import { ProductsComponent } from './pages/administrativo/products.component/products.component';
import { UsuariosComponent } from './pages/administrativo/usuarios/usuarios.component';
import { TiendaResolver } from './service/tienda.resolver';

// === RESOLVER PARA SLUG DE TIENDA ===

export const routes: Routes = [

  // ==================================================================
  // RUTAS PÚBLICAS CON SLUG DE TIENDA → http://localhost:4200/zapatik
  // ==================================================================
  {
    path: ':tiendaSlug',
    component: PublicLayaoutComponent,
    resolve: { tienda: TiendaResolver }, // ← Detecta el slug y lo guarda automáticamente
    children: [
      { path: '', component: MainTiendaComponent },                              // /zapatik
      { path: 'catalogo', component: CatalogoComponent },                        // /zapatik/catalogo
      { path: 'catalogo/:categoriaSlug', component: CatalogoComponent },        // /zapatik/catalogo/celulares
      { path: 'producto/:productoSlug', component: ProductoUnitarioComponent }, // /zapatik/producto/iphone-15
    ]
  },

  // ==================================================================
  // LOGIN Y PANEL ADMINISTRATIVO (sin slug, como lo tenías antes)
  // ==================================================================
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
      { path: 'usuarios', component: UsuariosComponent }
    ]
  },

  // ==================================================================
  // REDIRECCIONES
  // ==================================================================
  { path: '', redirectTo: '/zapatik', pathMatch: 'full' },     // ← Tienda por defecto al entrar
  { path: '**', redirectTo: '/zapatik' }                       // ← Cualquier ruta rara → zapatik
];