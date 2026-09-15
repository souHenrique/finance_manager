import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../models/budget.models';

@Injectable({ providedIn: 'root' })
export class BudgetApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  create(request: CreateBudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(this.apiUrl.build('budgets'), request);
  }

  findAll(): Observable<Budget[]> {
    return this.http.get<Budget[]>(this.apiUrl.build('budgets'));
  }

  findById(id: string): Observable<Budget> {
    return this.http.get<Budget>(this.apiUrl.build(`budgets/${encodeURIComponent(id)}`));
  }

  update(id: string, request: UpdateBudgetRequest): Observable<Budget> {
    return this.http.patch<Budget>(this.apiUrl.build(`budgets/${encodeURIComponent(id)}`), request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(this.apiUrl.build(`budgets/${encodeURIComponent(id)}`));
  }
}
