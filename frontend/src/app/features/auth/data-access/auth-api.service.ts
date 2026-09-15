import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiUrlService } from '../../../core/http/api-url.service';
import { User } from '../../../shared/models/user.models';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(ApiUrlService);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.apiUrl.build('auth/login'), request);
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl.build('auth/register'), request);
  }
}
