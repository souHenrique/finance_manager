import { Routes } from '@angular/router';

export const TRANSFER_ROUTES: Routes = [
  {
    path: 'new',
    title: 'Nova transferência | Finance Manager',
    loadComponent: () =>
      import('./pages/transfer-create-page/transfer-create-page').then(
        ({ TransferCreatePage }) => TransferCreatePage,
      ),
  },
];
