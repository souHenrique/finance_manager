import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: 'new',
    title: 'Nova conta | Finance Manager',
    loadComponent: () =>
      import('./pages/account-create-page/account-create-page').then(
        (module) => module.AccountCreatePageComponent,
      ),
  },
  {
    path: ':id/edit',
    title: 'Editar conta | Finance Manager',
    loadComponent: () =>
      import('./pages/account-edit-page/account-edit-page').then(
        (module) => module.AccountEditPageComponent,
      ),
  },
  {
    path: ':id',
    title: 'Detalhes da conta | Finance Manager',
    loadComponent: () =>
      import('./pages/account-detail-page/account-detail-page').then(
        (module) => module.AccountDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Contas | Finance Manager',
    loadComponent: () =>
      import('./pages/account-list-page/account-list-page').then(
        (module) => module.AccountListPage,
      ),
  },
];
