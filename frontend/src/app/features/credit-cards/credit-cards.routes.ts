import { Routes } from '@angular/router';

export const CREDIT_CARD_ROUTES: Routes = [
  {
    path: ':id',
    title: 'Detalhes do cartão | Finance Manager',
    loadComponent: () =>
      import('./pages/credit-card-detail-page/credit-card-detail-page').then(
        ({ CreditCardDetailPage }) => CreditCardDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Cartões de crédito | Finance Manager',
    loadComponent: () =>
      import('./pages/credit-card-list-page/credit-card-list-page').then(
        ({ CreditCardListPage }) => CreditCardListPage,
      ),
  },
];
