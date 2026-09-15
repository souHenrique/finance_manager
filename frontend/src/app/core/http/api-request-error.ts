import { ApiError } from '../../shared/models/api-error';

export class ApiRequestError extends Error {
  constructor(
    readonly apiError: ApiError,
    cause?: unknown,
  ) {
    super(apiError.message, { cause });
    this.name = 'ApiRequestError';
  }

  get status(): number {
    return this.apiError.status;
  }

  get code(): string {
    return this.apiError.code;
  }

  get fieldErrors() {
    return this.apiError.fieldErrors;
  }
}
