import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiError, FieldError } from '../../shared/models/api-error';

const GENERIC_SERVER_MESSAGE = 'Não foi possível concluir a operação. Tente novamente mais tarde.';

@Injectable({ providedIn: 'root' })
export class ApiErrorNormalizer {
  normalize(error: unknown, requestPath: string): ApiError {
    if (!(error instanceof HttpErrorResponse)) {
      return this.createFallbackError(500, requestPath);
    }

    const status = error.status;
    const payload = error.error;

    if (this.isApiError(payload)) {
      return {
        ...payload,
        status,
        message: status >= 500 ? GENERIC_SERVER_MESSAGE : payload.message,
        path: payload.path || this.removeQueryString(requestPath),
        fieldErrors: status >= 500 ? [] : payload.fieldErrors,
      };
    }

    return this.createFallbackError(status, requestPath);
  }

  private createFallbackError(status: number, requestPath: string): ApiError {
    const fallback = this.fallbackFor(status);

    return {
      timestamp: new Date().toISOString(),
      status,
      code: fallback.code,
      message: fallback.message,
      path: this.removeQueryString(requestPath),
      fieldErrors: [],
    };
  }

  private fallbackFor(status: number): {
    code: string;
    message: string;
  } {
    switch (status) {
      case 0:
        return {
          code: 'NETWORK_ERROR',
          message: 'Não foi possível se conectar ao servidor.',
        };

      case 400:
        return {
          code: 'INVALID_REQUEST',
          message: 'Verifique os dados informados.',
        };

      case 401:
        return {
          code: 'UNAUTHORIZED',
          message: 'Sua sessão não é válida ou expirou.',
        };

      case 404:
        return {
          code: 'RESOURCE_NOT_FOUND',
          message: 'O recurso solicitado não foi encontrado.',
        };

      case 409:
        return {
          code: 'CONFLICT',
          message: 'A operação entrou em conflito com o estado atual.',
        };

      default:
        return {
          code: 'INTERNAL_SERVER_ERROR',
          message: GENERIC_SERVER_MESSAGE,
        };
    }
  }

  private isApiError(value: unknown): value is ApiError {
    if (!this.isRecord(value)) {
      return false;
    }

    return (
      typeof value['timestamp'] === 'string' &&
      typeof value['status'] === 'number' &&
      typeof value['code'] === 'string' &&
      typeof value['message'] === 'string' &&
      typeof value['path'] === 'string' &&
      Array.isArray(value['fieldErrors']) &&
      value['fieldErrors'].every((fieldError) => this.isFieldError(fieldError))
    );
  }

  private isFieldError(value: unknown): value is FieldError {
    return (
      this.isRecord(value) &&
      typeof value['field'] === 'string' &&
      typeof value['message'] === 'string'
    );
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }

  private removeQueryString(path: string): string {
    return path.split('?')[0] ?? path;
  }
}
