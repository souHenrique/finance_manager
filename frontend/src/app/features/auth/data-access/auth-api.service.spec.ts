import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { User } from '../../../shared/models/user.models';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import { AuthApiService } from './auth-api.service';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(AuthApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve autenticar o usuário', () => {
    const payload: LoginRequest = {
      email: 'camila.souza@example.com',
      password: 'SenhaSegura123',
    };
    const response: AuthResponse = {
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    };

    service.login(payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne('/api/v1/auth/login');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve cadastrar o usuário', () => {
    const payload: RegisterRequest = {
      name: 'Camila Souza',
      email: 'camila.souza@example.com',
      password: 'SenhaSegura123',
    };
    const response: User = {
      id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
      name: payload.name,
      email: payload.email,
      createdAt: '2026-09-02T12:00:00Z',
      updatedAt: '2026-09-02T12:00:00Z',
    };

    service.register(payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne('/api/v1/auth/register');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });
});
