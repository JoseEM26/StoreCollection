// main.ts
import { bootstrapApplication } from '@angular/platform-browser';

// 1. Registro obligatorio de locales
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';     // español genérico (recomendado)
// import localeEsPE from '@angular/common/locales/es-PE'; // si prefieres Perú

registerLocaleData(localeEs, 'es');
// registerLocaleData(localeEsPE, 'es-PE'); // descomenta si quieres formato específico Perú

// 2. Registro explícito de Chart.js (muy importante en standalone + tree-shaking)
import {
  Chart,
  BarController,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
  Title
} from 'chart.js';

Chart.register(
  BarController,
  CategoryScale,     // ← soluciona el error de "category" scale
  LinearScale,
  Legend,
  Tooltip,
  Title
);

// 3. ng2-charts provider recomendado
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

// Tu aplicación
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [
    // Combinamos tu configuración existente con lo nuevo
    ...(appConfig.providers || []),
    provideCharts(withDefaultRegisterables()), // opcional pero recomendado
  ]
})
.catch((err) => console.error(err));