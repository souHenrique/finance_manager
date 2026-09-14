import { Routes } from '@angular/router';

export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Dashboard | Finance Manager',
    loadComponent: () =>
      import('./dashboard-page/dashboard-page').then(({ DashboardPage }) => DashboardPage),
  },
];
