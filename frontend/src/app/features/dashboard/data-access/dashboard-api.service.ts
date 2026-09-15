import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { Dashboard } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  get(): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.apiUrl.build('dashboard'));
  }
}
