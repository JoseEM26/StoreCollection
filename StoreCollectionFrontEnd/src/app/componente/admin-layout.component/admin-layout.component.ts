// src/app/componente/admin-layout.component/admin-layout.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';

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

  constructor(
    public auth: AuthService, // public para usar en template
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    // Cargar estado del sidebar
    const saved = localStorage.getItem('adminSidebarCollapsed');
    this.isSidebarCollapsed = saved === 'true';

    // Detectar móvil
    this.checkIfMobile();
    window.addEventListener('resize', () => this.checkIfMobile());

    // Título dinámico según ruta
    this.routerSub = this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map(() => {
          let child = this.route.root.firstChild;
          while (child?.firstChild) {
            child = child.firstChild;
          }
          return child?.snapshot.data['title'] || this.getTitleFromUrl(this.router.url);
        })
      )
      .subscribe(title => {
        this.pageTitle = title;
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

  // Fallback para títulos si no tienes data en rutas
  private getTitleFromUrl(url: string): string {
    const map: Record<string, string> = {
      '/admin/dashboard': 'Dashboard',
      '/admin/stores': 'Tiendas',
      '/admin/categories': 'Categorías',
      '/admin/products': 'Productos',
      '/admin/usuarios': 'Usuarios'
    };
    return map[url] || 'Administración';
  }
}