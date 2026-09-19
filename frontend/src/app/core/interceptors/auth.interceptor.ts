import { HttpInterceptorFn } from '@angular/common/http';

import { API_BASE_URL } from '../config/api-base-url';
import { inject } from '@angular/core';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(API_BASE_URL);

  const isApiRequest = request.url === apiBaseUrl || request.url.startsWith(`${apiBaseUrl}/`);

  if (!isApiRequest) {
    return next(request);
  }

  return next(request.clone({ withCredentials: true }));
};
