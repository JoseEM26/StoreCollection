// src/app/app.routes.ts
import { Routes } from '@angular/router';

// === COMPONENTES PÚBLICOS ===
import { DashboardPublicComponent } from './pages/publico/dashboard-public/dashboard-public.component';
import { PublicLayaoutComponent } from './componente/public-layaout/public-layaout.component';
import { MainTiendaComponent } from './pages/publico/main-tienda/main-tienda.component';
import { CatalogoComponent } from './pages/publico/catalogo/catalogo.component';
import { ProductoUnitarioComponent } from './pages/publico/producto-unitario/producto-unitario.component';

// === ADMIN ===
import { LoginComponent } from './pages/administrativo/login/login.component';
import { AdminLayoutComponent } from './componente/admin-layout.component/admin-layout.component';
import { DashboardComponent } from './pages/administrativo/dashboard/dashboard.component';
import { StoresComponent } from './pages/administrativo/stores.component/stores.component';
import { CategoriesComponent } from './pages/administrativo/categories.component/categories.component';
import { ProductsComponent } from './pages/administrativo/products.component/products.component';
import { UsuariosComponent } from './pages/administrativo/usuarios/usuarios.component';

// === GUARDS Y RESOLVERS ===
import { authGuard } from '../auth/auth.guard';
import { TiendaResolver } from './service/tienda.resolver';

export const routes: Routes = [
  // 1. HOME → Dashboard público con todas las tiendas
  {
    path: '',
    component: DashboardPublicComponent
  },

  // 2. RUTA PRINCIPAL POR SLUG DE TIENDA (LA MÁS IMPORTANTE)
  // Ejemplos válidos:
  // → /zapatik
  // → /zapatik/catalogo
  // → /zapatik/catalogo/zapatillas-hombre
  // → /zapatik/producto/nike-air-max-90
  {
    path: ':tiendaSlug',
    component: PublicLayaoutComponent,
    resolve: { tienda: TiendaResolver },        // ← Resolve crítico para obtener la tienda
    children: [
      { path: '', component: MainTiendaComponent },  // página principal de la tienda
      {
        path: 'catalogo',
        children: [
          { path: '', component: CatalogoComponent },                    // /:tiendaSlug/catalogo
          { path: ':categoriaSlug', component: CatalogoComponent }       // /:tiendaSlug/catalogo/hombre
        ]
      },
      { path: 'producto/:productoSlug', component: ProductoUnitarioComponent }
    ]
  },

  // 3. LOGIN
  {
    path: 'login',
    component: LoginComponent
  },

  // 4. PANEL ADMINISTRATIVO (protegido)
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

  // 5. WILDCARD → siempre al final
  { path: '**', redirectTo: '' }
];