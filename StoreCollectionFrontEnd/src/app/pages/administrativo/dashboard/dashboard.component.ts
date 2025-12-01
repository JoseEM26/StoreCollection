// src/app/admin/dashboard/dashboard.component.ts
import { Component, ViewChild, ElementRef, AfterViewInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements AfterViewInit {
  @ViewChild('revenueChart') revenueChart!: ElementRef<HTMLCanvasElement>;

  darkMode = signal<boolean>(false);

  stats = signal([
    { icon: 'bi bi-box-seam-fill', label: 'Productos', value: 2847, prefix: '', change: 18, gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
    { icon: 'bi bi-shop-fill', label: 'Tiendas Activas', value: 142, prefix: '', change: 34, gradient: 'linear-gradient(135deg, #f093fb, #f5576c)' },
    { icon: 'bi bi-people-fill', label: 'Usuarios', value: 8921, prefix: '', change: 27, gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
    { icon: 'bi bi-currency-dollar', label: 'Ingresos Mes', value: 89_421, prefix: '$', change: 42, gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
  ]);

  recentStores = signal([
    { name: 'Viral Beauty', domain: 'viralbeauty.store', active: true },
    { name: 'TechTrend', domain: 'techtrend.store', active: true },
    { name: 'Fashion Hub', domain: 'fashionhub.store', active: true },
    { name: 'GadgetZone', domain: 'gadgetzone.store', active: false },
  ]);

  recentActivity = signal([
    { icon: 'bi bi-shop', color: '#e0e7ff', text: 'Nueva tienda: MakeupPro', time: 'Hace 5 min' },
    { icon: 'bi bi-cart-check', color: '#d1fae5', text: 'Venta de $249 en Ropita Viral', time: 'Hace 12 min' },
    { icon: 'bi bi-person-plus', color: '#fef3c7', text: 'Nuevo admin registrado', time: 'Hace 1 hora' },
  ]);

  constructor() {
    // Persistir modo oscuro
    const saved = localStorage.getItem('darkMode');
    if (saved) this.darkMode.set(JSON.parse(saved));
  }

  toggleDarkMode() {
    this.darkMode.update(v => {
      const newVal = !v;
      localStorage.setItem('darkMode', JSON.stringify(newVal));
      return newVal;
    });
  }

  ngAfterViewInit() {
    this.createRevenueChart();
    this.createSparklines();
  }

  createRevenueChart() {
    const ctx = this.revenueChart.nativeElement.getContext('2d')!;
    new Chart(ctx, {
      type: 'line',
      data: {
        labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'],
        datasets: [{
          label: 'Ingresos 2025',
          data: [12_400, 19_200, 24_100, 32_800, 41_200, 58_900, 72_300, 89_421],
          borderColor: '#6366f1',
          backgroundColor: 'rgba(99, 102, 241, 0.1)',
          tension: 0.4,
          fill: true,
          pointBackgroundColor: '#6366f1',
          pointRadius: 5,
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, grid: { display: false } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  createSparklines() {
    this.stats().forEach((_, i) => {
      const canvas = document.getElementById(`sparkline-${i}`) as HTMLCanvasElement;
      if (!canvas) return;
      const data = [30, 45, 38, 60, 55, 70, 65, 80][i] || 70;
      new Chart(canvas, {
        type: 'line',
        data: { datasets: [{ data: Array(7).fill(0).map(() => Math.random() * 30 + data), borderColor: '#6366f1', tension: 0.4, fill: false, pointRadius: 0 }] },
        options: { plugins: { legend: { display: false } }, scales: { x: { display: false }, y: { display: false } } }
      });
    });
  }
}