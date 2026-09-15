import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import {
  Account,
  CreateAccountRequest,
  UpdateAccountRequest,
  UpdateAccountStatusRequest,
} from '../models/account.models';

@Injectable({ providedIn: 'root' })
export class AccountApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(this.apiUrl.build('accounts'), request);
  }

  findAll(): Observable<Account[]> {
    return this.http.get<Account[]>(this.apiUrl.build('accounts'));
  }

  findById(id: string): Observable<Account> {
    return this.http.get<Account>(this.apiUrl.build(`accounts/${encodeURIComponent(id)}`));
  }

  update(id: string, request: UpdateAccountRequest): Observable<Account> {
    return this.http.patch<Account>(
      this.apiUrl.build(`accounts/${encodeURIComponent(id)}`),
      request,
    );
  }

  updateStatus(id: string, request: UpdateAccountStatusRequest): Observable<Account> {
    return this.http.patch<Account>(
      this.apiUrl.build(`accounts/${encodeURIComponent(id)}/status`),
      request,
    );
  }
}
