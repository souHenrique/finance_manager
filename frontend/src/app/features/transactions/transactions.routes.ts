import { Routes } from '@angular/router';

export const TRANSACTION_ROUTES: Routes = [
  {
    path: 'new',
    title: 'Nova transação | Finance Manager',
    loadComponent: () =>
      import('./pages/transaction-create-page/transaction-create-page').then(
        ({ TransactionCreatePage }) => TransactionCreatePage,
      ),
  },
  {
    path: ':id/edit',
    title: 'Editar transação | Finance Manager',
    loadComponent: () =>
      import('./pages/transaction-edit-page/transaction-edit-page').then(
        ({ TransactionEditPage }) => TransactionEditPage,
      ),
  },
  {
    path: ':id',
    title: 'Detalhes da transação | Finance Manager',
    loadComponent: () =>
      import('./pages/transaction-detail-page/transaction-detail-page').then(
        ({ TransactionDetailPage }) => TransactionDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Transações | Finance Manager',
    loadComponent: () =>
      import('./pages/transaction-list-page/transaction-list-page').then(
        ({ TransactionListPage }) => TransactionListPage,
      ),
  },
];
