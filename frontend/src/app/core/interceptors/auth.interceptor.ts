import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionService } from '../auth/session.service';
import { API_BASE_URL } from '../config/api-base-url';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(API_BASE_URL).replace(/\/+$/, '');
  const accessToken = inject(SessionService).accessToken();

  const isApiRequest = request.url === apiBaseUrl || request.url.startsWith(`${apiBaseUrl}/`);

  if (!isApiRequest || !accessToken) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
    }),
  );
};
