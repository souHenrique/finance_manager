import { Routes } from '@angular/router';

export const REPORT_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Relatórios | Nummo',
    loadComponent: () =>
      import('./reports-page/reports-page').then(({ ReportsPage }) => ReportsPage),
  },
];
