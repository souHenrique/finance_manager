import { Routes } from '@angular/router';

export const CREDIT_CARD_ROUTES: Routes = [
  {
    path: 'new',
    title: 'Novo cartão | Nummo',
    loadComponent: () =>
      import('./pages/credit-card-create-page/credit-card-create-page').then(
        ({ CreditCardCreatePage }) => CreditCardCreatePage,
      ),
  },
  {
    path: ':id/purchases/new',
    title: 'Nova compra no cartão | Nummo',
    loadComponent: () =>
      import('./pages/credit-card-purchase-create-page/credit-card-purchase-create-page').then(
        ({ CreditCardPurchaseCreatePage }) => CreditCardPurchaseCreatePage,
      ),
  },
  {
    path: ':id/edit',
    title: 'Editar cartão | Nummo',
    loadComponent: () =>
      import('./pages/credit-card-edit-page/credit-card-edit-page').then(
        ({ CreditCardEditPage }) => CreditCardEditPage,
      ),
  },
  {
    path: ':id',
    title: 'Detalhes do cartão | Nummo',
    loadComponent: () =>
      import('./pages/credit-card-detail-page/credit-card-detail-page').then(
        ({ CreditCardDetailPage }) => CreditCardDetailPage,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Cartões de crédito | Nummo',
    loadComponent: () =>
      import('./pages/credit-card-list-page/credit-card-list-page').then(
        ({ CreditCardListPage }) => CreditCardListPage,
      ),
  },
];
