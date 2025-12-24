// src/app/app.routes.ts
import { Routes } from '@angular/router';

// === PÚBLICO ===
import { DashboardPublicComponent } from './pages/publico/dashboard-public/dashboard-public.component';
import { PublicLayaoutComponent } from './componente/public-layaout/public-layaout.component';
import { MainTiendaComponent } from './pages/publico/main-tienda/main-tienda.component';
import { CatalogoComponent } from './pages/publico/catalogo/catalogo.component';
import { ProductoUnitarioComponent } from './pages/publico/producto-unitario/producto-unitario.component';
import { ConocenosComponent } from './pages/publico/conocenos/conocenos.component';

// === ADMIN ===
import { LoginComponent } from './pages/administrativo/login/login.component';
import { AdminLayoutComponent } from './componente/admin-layout.component/admin-layout.component';
import { DashboardComponent } from './pages/administrativo/dashboard/dashboard.component';
import { UsuariosComponent } from './pages/administrativo/usuarios/usuarios.component';
import { StoresComponent } from './pages/administrativo/stores.component/stores.component';
import { CategoriesComponent } from './pages/administrativo/categories.component/categories.component';
import { ProductsComponent } from './pages/administrativo/products.component/products.component';
import { PlanesComponent } from './pages/administrativo/planes/planes.component';

// === PÁGINAS ESPECIALES ===
import { SuscripcionExpiradaComponentComponent } from './componente/suscripcion-expirada-component/suscripcion-expirada-component.component';

// === GUARDS & RESOLVERS ===
import { TiendaResolver } from './service/tienda.resolver';
import { authGuard } from '../auth/auth.guard';
import { tiendaExistsGuard } from '../auth/tienda-exists.guard';
import { resourceActiveGuard } from '../auth/resource-active.guard'; // ← IMPORTANTE: el guard para productos inactivos
import { CarritoComponent } from './componente/carrito/carrito.component';
import { BoletaComponent } from './pages/administrativo/boleta/boleta.component';

export const routes: Routes = [

  // 1. RUTAS ESTÁTICAS
  { path: '', component: DashboardPublicComponent, title: 'Store Collection' },
  { path: 'login', component: LoginComponent, title: 'Iniciar Sesión' },

  // Página de suscripción expirada o tienda inactiva
  {
    path: 'suscripcion-expirada',
    component: SuscripcionExpiradaComponentComponent,
    title: 'Suscripción Expirada'
  },

  // 2. PANEL ADMIN (protegido por authGuard)
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard' },
      { path: 'stores', component: StoresComponent, title: 'Tiendas' },
      { path: 'categories', component: CategoriesComponent, title: 'Categorías' },
      { path: 'products', component: ProductsComponent, title: 'Productos' },
      { path: 'planes', component: PlanesComponent, title: 'Planes' },
      { path: 'usuarios', component: UsuariosComponent, title: 'Usuarios' },
      { path: 'boletas', component: BoletaComponent, title: 'Boletas' },
    ]
  },

  {
    path: ':tiendaSlug',
    canMatch: [tiendaExistsGuard], 
    component: PublicLayaoutComponent,
    resolve: { tienda: TiendaResolver },
    children: [
      { path: '', component: MainTiendaComponent },
      { path: 'conocenos', component: ConocenosComponent },
      { path: 'carrito', component: CarritoComponent, title: 'Mi Carrito' }, 
      {
        path: 'catalogo',
        children: [
          { path: '', component: CatalogoComponent },
          { path: ':categoriaSlug', component: CatalogoComponent } // filtro por categoría (opcional)
        ]
      },

      {
        path: 'producto/:productoSlug',
        canMatch: [resourceActiveGuard], // ← Aquí bloquea si el producto está inactivo
        component: ProductoUnitarioComponent
      },

    ]
  },

  // 4. 404 → redirige al home
  { path: '**', redirectTo: '' }
];