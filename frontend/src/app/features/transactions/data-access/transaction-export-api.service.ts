import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { TransactionFilters } from '../models/transaction.models';
import { buildTransactionFilterParams } from './transaction-http-params';

@Injectable({ providedIn: 'root' })
export class TransactionExportApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  download(filters: TransactionFilters = {}): Observable<Blob> {
    return this.http.get(this.apiUrl.build('exports/transactions.csv'), {
      params: buildTransactionFilterParams(filters),
      responseType: 'blob',
    });
  }
}
