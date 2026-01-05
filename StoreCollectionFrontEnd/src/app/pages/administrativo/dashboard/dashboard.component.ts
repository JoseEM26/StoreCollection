import { Component, OnInit, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType, ChartDataset } from 'chart.js';
import Swal from 'sweetalert2';

import {
  DashboardService,
  DashboardOverview,
  RevenuePorEstado,
  TiendaDashboardDto,
  PageResponse,
  PlanUsageDto
} from '../../../service/service-admin/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  overview = signal<DashboardOverview | null>(null);
  revenuePorEstado = signal<RevenuePorEstado>({});
  tiendasRecientes = signal<TiendaDashboardDto[]>([]);
  planUsage = signal<PlanUsageDto | null>(null);

  loading = signal(true);
  error = signal<string | null>(null);
  darkMode = signal<boolean>(false);

  currentDate = new Date();

  // Bandera para evitar múltiples alertas
  private alertsShown = false;

  // Configuración del gráfico
  public barChartType: ChartType = 'bar';

  public barChartData = signal<ChartData<'bar'>>({
    labels: ['Ingresos por Estado'],
    datasets: []
  });

  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y' as const,
    plugins: {
      legend: { position: 'bottom', labels: { padding: 20, font: { size: 13 } } },
      tooltip: {
        callbacks: {
          label: (context) => {
            const value = context.parsed.x ?? 0;
            return `S/ ${value.toLocaleString('es-PE', { minimumFractionDigits: 2 })}`;
          }
        }
      }
    },
    scales: {
      x: {
        stacked: true,
        grid: { display: false },
        ticks: { callback: (v) => `S/ ${Number(v).toLocaleString('es-PE')}` }
      },
      y: {
        stacked: true,
        grid: { display: false }
      }
    }
  };

  private readonly ESTADOS = {
    ATENDIDA:   { label: 'Atendidas / Pagadas', color: '#10b981' },
    PENDIENTE:  { label: 'Pendientes',          color: '#f59e0b' },
    CANCELADA:  { label: 'Canceladas',          color: '#ef4444' }
  } as const;

  constructor(
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {
    const savedDark = localStorage.getItem('darkMode');
    if (savedDark !== null) {
      this.darkMode.set(JSON.parse(savedDark));
    }
  }

  ngOnInit(): void {
    this.loadAllData();
  }

  private async loadAllData(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const [overview, revenue, tiendas, planUsage] = await Promise.all([
        this.dashboardService.getOverview().toPromise().catch(() => null),
        this.dashboardService.getRevenuePorEstado().toPromise().catch(() => ({})),
        this.dashboardService.getTiendas(0, 5).toPromise().catch(() => []),
        this.dashboardService.getPlanUsage().toPromise().catch(() => null)
      ]);

      if (overview) this.overview.set(overview);
      if (revenue) {
        this.revenuePorEstado.set(revenue);
        this.updateChart(revenue);
      }
      if (planUsage) {
        this.planUsage.set(planUsage);
        this.checkRenewalAlerts(planUsage); // ← Aquí se muestran las alertas
      }

      const tiendasArray = Array.isArray(tiendas) ? tiendas : (tiendas as PageResponse<TiendaDashboardDto>)?.content || [];
      this.tiendasRecientes.set(tiendasArray.slice(0, 5));

    } catch (err) {
      console.error('Error cargando dashboard:', err);
      this.error.set('No pudimos cargar todos los datos del dashboard');
      Swal.fire({
        icon: 'error',
        title: 'Error',
        text: 'Hubo un problema al cargar los datos. Intenta nuevamente.',
        confirmButtonColor: '#3085d6'
      });
    } finally {
      this.loading.set(false);
      this.cdr.detectChanges();
    }
  }

  // Muestra SweetAlert solo como aviso (sin botón "Renovar ahora")
  private checkRenewalAlerts(usage: PlanUsageDto): void {
    if (this.alertsShown || !usage.fechaProximaRenovacion) return;

    const dias = usage.diasRestantesRenovacion;

    if (dias <= 0) {
      // Ya pasó la fecha → Alarma grave de suspensión
      this.alertsShown = true;
      Swal.fire({
        icon: 'error',
        title: '¡Plan vencido!',
        html: `La renovación de tu plan (${usage.planNombre}) pasó la fecha límite.<br>
               <strong>Tu tienda será suspendida pronto</strong> si no renuevas.<br><br>
               Fecha límite: <strong>${new Date(usage.fechaProximaRenovacion).toLocaleDateString('es-PE')}</strong>`,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#dc3545',
        allowOutsideClick: true
      });
    } else if (dias <= 7) {
      // Faltan 7 días o menos → Alerta informativa
      this.alertsShown = true;
      Swal.fire({
        icon: 'warning',
        title: '¡Renueva pronto!',
        html: `Te quedan <strong>${dias} día${dias === 1 ? '' : 's'}</strong> para renovar tu plan (${usage.planNombre}).<br>
               Fecha límite: <strong>${new Date(usage.fechaProximaRenovacion).toLocaleDateString('es-PE')}</strong>`,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#fd7e14',
        allowOutsideClick: true
      });
    }
  }

  private updateChart(data: RevenuePorEstado): void {
    const datasets: ChartDataset<'bar'>[] = [];

    for (const [key, config] of Object.entries(this.ESTADOS)) {
      const valor = Number(data[key] || '0');
      if (valor > 0) {
        datasets.push({
          label: config.label,
          data: [valor],
          backgroundColor: config.color,
          borderRadius: 8,
          barThickness: 32,
          borderSkipped: false
        });
      }
    }

    this.barChartData.set({
      labels: ['Ingresos por Estado'],
      datasets
    });
  }

  getProgressClass(porcentaje: number): string {
    if (porcentaje >= 90) return 'bg-danger';
    if (porcentaje >= 70) return 'bg-warning';
    if (porcentaje >= 40) return 'bg-info';
    return 'bg-success';
  }

  getAvatarUrl(name: string): string {
    return `https://api.dicebear.com/7.x/shapes/svg?seed=${encodeURIComponent(name)}`;
  }

  get stats() {
    const o = this.overview();
    if (!o) return [];

    const revenue = Number(o.revenueTotal || '0');

    return [
      { icon: 'bi-shop-window', label: 'Tiendas', value: o.totalTiendas, type: 'number', gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
      { icon: 'bi-cart-check-fill', label: 'Órdenes', value: o.totalBoletas, type: 'number', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)' },
      { icon: 'bi-currency-dollar', label: 'Ingresos', value: revenue, prefix: 'S/ ', type: 'currency', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)' },
      { icon: 'bi-trophy-fill', label: 'Rol', value: o.rol === 'ADMIN' ? 'Administrador' : 'Propietario', type: 'text', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)' }
    ];
  }
}