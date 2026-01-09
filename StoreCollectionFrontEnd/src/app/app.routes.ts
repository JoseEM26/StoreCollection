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
import { resourceActiveGuard } from '../auth/resource-active.guard'; // ← IMPORTANTE: el guard para productos inactivos
import { CarritoComponent } from './componente/carrito/carrito.component';
import { BoletaComponent } from './pages/administrativo/boleta/boleta.component';
import { tiendaAccessGuard } from '../auth/tienda-access.guard';
import { planActiveGuard } from '../auth/plan-active.guard';
import { adminOnlyGuard } from '../auth/admin-only.guard';
import { AtributoComponent } from './pages/administrativo/atributo/atributo.component';

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

{
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard], // autenticación general
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard' },

      // Rutas EXCLUSIVAS para ADMIN
      {
        path: 'usuarios',
        component: UsuariosComponent,
        canActivate: [planActiveGuard, adminOnlyGuard], // ← Añadimos el nuevo guard
        title: 'Usuarios'
      },
      {
        path: 'planes',
        component: PlanesComponent,
        canActivate: [planActiveGuard, adminOnlyGuard], // ← Aquí también
        title: 'Planes'
      },

      // Rutas permitidas para OWNER y ADMIN (solo planActiveGuard)
      { path: 'stores', component: StoresComponent, canActivate: [planActiveGuard], title: 'Tiendas' },
      { path: 'categories', component: CategoriesComponent, canActivate: [planActiveGuard], title: 'Categorías' },
      { path: 'products', component: ProductsComponent, canActivate: [planActiveGuard], title: 'Productos' },
      { path: 'atributos', component: AtributoComponent, canActivate: [planActiveGuard], title: 'Atributos' },
      { path: 'boletas', component: BoletaComponent, canActivate: [planActiveGuard], title: 'Boletas' },
    ]
  },

  {
    path: ':tiendaSlug',
    canMatch: [tiendaAccessGuard], 
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