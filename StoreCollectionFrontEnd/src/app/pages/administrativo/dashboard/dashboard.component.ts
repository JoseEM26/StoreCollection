// src/app/admin/dashboard/dashboard.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  stats = signal([
    { icon: 'bi-box-seam', label: 'Productos activos', value: 1_248, color: 'primary', change: '+12%' },
    { icon: 'bi-shop', label: 'Tiendas activas', value: 87, color: 'success', change: '+23%' },
    { icon: 'bi-tags', label: 'Categorías', value: 42, color: 'info', change: '+8%' },
    { icon: 'bi-currency-dollar', label: 'Ingresos este mes', value: 48_291, prefix: '$', color: 'warning', change: '+31%' },
  ]);

  recentStores = signal([
    { name: 'Ropita Viral', domain: 'ropita.storecollection.com', status: 'active' },
    { name: 'TechZone Pro', domain: 'techzone.storecollection.com', status: 'active' },
    { name: 'Belleza TikTok', domain: 'belleza.storecollection.com', status: 'inactive' },
  ]);
}