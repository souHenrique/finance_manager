import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: ':id',
    title: 'Detalhes da conta | Finance Manager',
    loadComponent: () =>
      import('./pages/account-detail-page/account-detail-page').then(
        ({ AccountDetailPage }) => AccountDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Contas | Finance Manager',
    loadComponent: () =>
      import('./pages/account-list-page/account-list-page').then(
        ({ AccountListPage }) => AccountListPage,
      ),
  },
];
