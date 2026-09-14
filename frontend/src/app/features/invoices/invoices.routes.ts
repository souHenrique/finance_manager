import { Routes } from '@angular/router';

export const INVOICE_ROUTES: Routes = [
  {
    path: ':id',
    title: 'Detalhes da fatura | Finance Manager',
    loadComponent: () =>
      import('./pages/invoice-detail-page/invoice-detail-page').then(
        ({ InvoiceDetailPage }) => InvoiceDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Faturas | Finance Manager',
    loadComponent: () =>
      import('./pages/invoice-list-page/invoice-list-page').then(
        ({ InvoiceListPage }) => InvoiceListPage,
      ),
  },
];
