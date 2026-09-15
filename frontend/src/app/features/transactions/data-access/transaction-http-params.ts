import { HttpParams } from '@angular/common/http';
import { PageQuery } from '../../../shared/models/pagination';
import { TransactionFilters, TransactionQuery } from '../models/transaction.models';

export function buildTransactionFilterParams(filters: TransactionFilters): HttpParams {
  let params = new HttpParams();

  const values: [string, string | number | undefined][] = [
    ['startDate', filters.startDate],
    ['endDate', filters.endDate],
    ['categoryId', filters.categoryId],
    ['accountId', filters.accountId],
    ['creditCardId', filters.creditCardId],
    ['type', filters.type],
    ['status', filters.status],
    ['minAmount', filters.minAmount],
    ['maxAmount', filters.maxAmount],
    ['description', filters.description],
  ];

  for (const [name, value] of values) {
    if (value !== undefined && value !== null && value !== '') {
      params = params.set(name, String(value));
    }
  }

  return params;
}

export function buildTransactionQueryParams(query: TransactionQuery): HttpParams {
  return appendPageParams(buildTransactionFilterParams(query), query);
}

function appendPageParams(initialParams: HttpParams, query: PageQuery): HttpParams {
  let params = initialParams;

  if (query.page !== undefined) {
    params = params.set('page', String(query.page));
  }

  if (query.size !== undefined) {
    params = params.set('size', String(query.size));
  }

  if (query.sort !== undefined) {
    const sortValues = Array.isArray(query.sort) ? query.sort : [query.sort];

    for (const sort of sortValues) {
      params = params.append('sort', sort);
    }
  }

  return params;
}
