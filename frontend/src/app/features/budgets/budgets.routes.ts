import { Routes } from '@angular/router';

export const BUDGET_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Orçamentos | Finance Manager',
    loadComponent: () =>
      import('./budget-list-page/budget-list-page').then(({ BudgetListPage }) => BudgetListPage),
  },
];
