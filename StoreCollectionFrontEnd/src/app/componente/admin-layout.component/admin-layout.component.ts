// src/app/admin/admin-layout/admin-layout.component.ts
import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent implements OnInit {
  isSidebarCollapsed = false;
  isMobile = false;

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    // Recuperar estado del sidebar
    const saved = localStorage.getItem('sidebarCollapsed');
    this.isSidebarCollapsed = saved === 'true';

    // Detectar si es móvil
    this.checkIfMobile();
    window.addEventListener('resize', () => this.checkIfMobile());
  }

  checkIfMobile() {
    this.isMobile = window.innerWidth < 992;
    if (this.isMobile) {
      this.isSidebarCollapsed = true; // En móvil siempre empieza cerrado
    }
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    localStorage.setItem('sidebarCollapsed', this.isSidebarCollapsed.toString());
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}