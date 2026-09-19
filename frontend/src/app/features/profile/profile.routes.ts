import { Routes } from '@angular/router';

export const PROFILE_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Perfil | Nummo',
    loadComponent: () =>
      import('./profile-page/profile-page').then(({ ProfilePage }) => ProfilePage),
  },
];
