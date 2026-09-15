import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from '../config/api-base-url';
import { ApiErrorNormalizer } from '../http/api-error-normalizer';
import { ApiRequestError } from '../http/api-request-error';
import { GlobalHttpErrorHandler } from '../http/global-http-error-handler';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(API_BASE_URL);

  if (!request.url.startsWith(apiBaseUrl)) {
    return next(request);
  }

  const normalizer = inject(ApiErrorNormalizer);
  const handler = inject(GlobalHttpErrorHandler);

  return next(request).pipe(
    catchError((cause: unknown) => {
      const apiError = normalizer.normalize(cause, request.urlWithParams);

      handler.handle(apiError, request);

      return throwError(() => new ApiRequestError(apiError, cause));
    }),
  );
};
