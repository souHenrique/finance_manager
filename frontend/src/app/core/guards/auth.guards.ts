import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';

import { SessionService } from '../auth/session.service';

const checkAuthentication = (requestedUrl: string) => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.hasValidSession()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: {
      returnUrl: requestedUrl,
    },
  });
};

export const authGuard: CanActivateFn = (_route, state) => {
  return checkAuthentication(state.url);
};

export const authChildGuard: CanActivateChildFn = (_childRoute, state) => {
  return checkAuthentication(state.url);
};
