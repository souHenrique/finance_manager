import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

import { SessionService } from '../auth/session.service';
import { API_BASE_URL } from '../config/api-base-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const apiBaseUrl = inject(API_BASE_URL);

  const isApiRequest =
    request.url === apiBaseUrl ||
    request.url.startsWith(`${apiBaseUrl}/`);

  const isPublicAuthRequest =
    request.url === `${apiBaseUrl}/auth/login` ||
    request.url === `${apiBaseUrl}/auth/register`;

  const authorization = session.getAuthorizationHeader();

  if (!isApiRequest || isPublicAuthRequest || !authorization) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: authorization,
      },
    }),
  );
};
