// src/app/componente/admin-layout.component/admin-layout.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, map } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';

interface MenuItem {
  route: string;
  icon: string;
  label: string;
  allowedRoles: ('ADMIN' | 'OWNER')[];
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  isSidebarCollapsed = false;
  isMobile = false;
  pageTitle = 'Dashboard';
  private routerSub!: Subscription;

  // Menú dinámico según rol
  menuItems: MenuItem[] = [
    { route: '/admin/dashboard',   icon: 'bi-speedometer2', label: 'Dashboard',   allowedRoles: ['ADMIN', 'OWNER'] },
    { route: '/admin/stores',      icon: 'bi-shop',         label: 'Tiendas',     allowedRoles: ['ADMIN', 'OWNER'] },
    { route: '/admin/categories',  icon: 'bi-grid-3x3-gap', label: 'Categorías',  allowedRoles: ['ADMIN', 'OWNER'] },
    { route: '/admin/products',    icon: 'bi-box-seam',     label: 'Productos',   allowedRoles: ['ADMIN', 'OWNER'] },
    { route: '/admin/usuarios',    icon: 'bi-people',       label: 'Usuarios',    allowedRoles: ['ADMIN'] }, // ← Solo ADMIN
    { route: '/admin/planes',    icon: 'bi-people',       label: 'Planes',    allowedRoles: ['ADMIN'] }, // ← Solo ADMIN
    { route: '/admin/boletas',    icon: 'bi-people',       label: 'Boletas',    allowedRoles: ['ADMIN', 'OWNER'] }, // ← Solo ADMIN
    { route: '/admin/atributos',    icon: 'bi-people',       label: 'Atributos',    allowedRoles: ['ADMIN', 'OWNER'] } // ← Solo ADMIN
  ];

  constructor(
    public auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    // Estado del sidebar
    const saved = localStorage.getItem('adminSidebarCollapsed');
    this.isSidebarCollapsed = saved === 'true';

    // Detectar móvil
    this.checkIfMobile();
    window.addEventListener('resize', () => this.checkIfMobile());

    // Título dinámico
    this.routerSub = this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => {
          let child = this.route.root.firstChild;
          while (child?.firstChild) child = child.firstChild;
          return child?.snapshot.data['title'] || this.getTitleFromUrl(this.router.url);
        })
      )
      .subscribe(title => this.pageTitle = title);

    // PROTECCIÓN: Si OWNER intenta entrar a ruta prohibida → redirigir
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        if (!this.auth.isAdmin() && this.isRestrictedRoute(event.urlAfterRedirects)) {
          this.router.navigate(['/admin/dashboard']);
        }
      });
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
  }

  checkIfMobile() {
    this.isMobile = window.innerWidth < 992;
    if (this.isMobile) this.isSidebarCollapsed = true;
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    localStorage.setItem('adminSidebarCollapsed', this.isSidebarCollapsed.toString());
  }

  logout() {
    this.auth.logout();
  }

  // Verifica si la ruta actual está restringida para OWNER
  private isRestrictedRoute(url: string): boolean {
    return url.includes('/admin/usuarios');
  }

  // Fallback de títulos
  private getTitleFromUrl(url: string): string {
    const map: Record<string, string> = {
      '/admin/dashboard': 'Dashboard',
      '/admin/stores': 'Tiendas',
      '/admin/categories': 'Categorías',
      '/admin/products': 'Productos',
      '/admin/usuarios': 'Usuarios'
    };
    return map[url.split('?')[0]] || 'Administración';
  }

  // Helper para mostrar ítem en menú
  isMenuItemVisible(item: MenuItem): boolean {
    return item.allowedRoles.includes(this.auth.currentUser()?.rol as any);
  }
}