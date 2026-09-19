import { Routes } from '@angular/router';

export const CATEGORY_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Categorias | Nummo',
    loadComponent: () =>
      import('./category-list-page/category-list-page').then(
        ({ CategoryListPage }) => CategoryListPage,
      ),
  },
];
