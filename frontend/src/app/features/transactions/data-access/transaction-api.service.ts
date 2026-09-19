import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { PageResponse } from '../../../shared/models/pagination';
import {
  CreateTransactionRequest,
  Transaction,
  TransactionInstallmentDetails,
  TransactionListItem,
  TransactionQuery,
  UpdateTransactionRequest,
} from '../models/transaction.models';
import { buildTransactionQueryParams } from './transaction-http-params';

@Injectable({ providedIn: 'root' })
export class TransactionApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl.build('transactions'), request);
  }

  findAll(query: TransactionQuery = {}): Observable<PageResponse<TransactionListItem>> {
    return this.http.get<PageResponse<TransactionListItem>>(this.apiUrl.build('transactions'), {
      params: buildTransactionQueryParams(query),
    });
  }

  findById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(this.apiUrl.build(`transactions/${encodeURIComponent(id)}`));
  }

  findInstallmentDetails(id: string): Observable<TransactionInstallmentDetails> {
    return this.http.get<TransactionInstallmentDetails>(
      this.apiUrl.build(`transactions/${encodeURIComponent(id)}/installments`),
    );
  }

  update(id: string, request: UpdateTransactionRequest): Observable<Transaction> {
    return this.http.patch<Transaction>(
      this.apiUrl.build(`transactions/${encodeURIComponent(id)}`),
      request,
    );
  }

  cancel(id: string): Observable<Transaction> {
    return this.http.post<Transaction>(
      this.apiUrl.build(`transactions/${encodeURIComponent(id)}/cancel`),
      null,
    );
  }
}
