import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { Transaction } from '../../transactions/models/transaction.models';
import { CreateTransferRequest } from '../models/transfer.models';

@Injectable({ providedIn: 'root' })
export class TransferApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateTransferRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl.build('transfers'), request);
  }
}
