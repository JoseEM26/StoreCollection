// src/app/app.routes.ts
import { Routes } from '@angular/router';

// === DASHBOARD PÚBLICO (NUEVO) ===

// === PÁGINAS PÚBLICAS CON SLUG DE TIENDA ===
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
import { DashboardPublicComponent } from './pages/publico/dashboard-public/dashboard-public.component';

export const routes: Routes = [

  // ================================================
  // 1. RUTA RAÍZ → DASHBOARD PÚBLICO (LISTADO DE TIENDAS)
  // ================================================
  {
    path: '',
    component: DashboardPublicComponent
  },

  // ================================================
  // 2. RUTAS CON SLUG → TIENDAS INDIVIDUALES
  //    Ej: /accesorios-cel, /zapatik, /belleza-natural
  // ================================================
  {
    path: ':tiendaSlug',
    component: PublicLayaoutComponent,
    resolve: { tienda: TiendaResolver },
    children: [
      { path: '', component: MainTiendaComponent },                              // /accesorios-cel
      { path: 'catalogo', component: CatalogoComponent },                        // /accesorios-cel/catalogo
      { path: 'catalogo/:categoriaSlug', component: CatalogoComponent },        // /accesorios-cel/catalogo/celulares
      { path: 'producto/:productoSlug', component: ProductoUnitarioComponent }, // /accesorios-cel/producto/iphone-15
    ]
  },

  // ================================================
  // 3. LOGIN Y PANEL ADMINISTRATIVO
  // ================================================
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

  // ================================================
  // 4. RUTAS DE RESPALDO (opcional)
  // ================================================
  { path: '**', redirectTo: '' }  // Cualquier ruta desconocida → al dashboard público
];