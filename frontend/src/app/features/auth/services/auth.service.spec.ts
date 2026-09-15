import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { SessionService } from '../../../core/auth/session.service';
import { User } from '../../../shared/models/user.models';
import { AuthApiService } from '../data-access/auth-api.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let authApi: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
  };
  let session: {
    start: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    authApi = {
      login: vi.fn(),
      register: vi.fn(),
    };
    session = {
      start: vi.fn(),
      clear: vi.fn(),
    };
    router = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        {
          provide: AuthApiService,
          useValue: authApi,
        },
        {
          provide: SessionService,
          useValue: session,
        },
        {
          provide: Router,
          useValue: router,
        },
      ],
    });

    service = TestBed.inject(AuthService);
  });

  it('should create a session after a successful login', () => {
    const request: LoginRequest = {
      email: 'camila.souza@example.com',
      password: 'SenhaSegura123',
    };
    const response: AuthResponse = {
      token: 'jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    };

    authApi.login.mockReturnValue(of(response));

    service.login(request).subscribe((result) => {
      expect(result).toEqual(response);
    });

    expect(authApi.login).toHaveBeenCalledWith(request);
    expect(session.start).toHaveBeenCalledWith(
      response.token,
      response.tokenType,
      response.expiresIn,
    );
  });

  it('should register without creating a session', () => {
    const request: RegisterRequest = {
      name: 'Camila Souza',
      email: 'camila.souza@example.com',
      password: 'SenhaSegura123',
    };
    const response: User = {
      id: 'f02e76b3-8d53-42dd-b4c5-43fcda2d3d84',
      name: request.name,
      email: request.email,
      createdAt: '2026-09-15T12:00:00Z',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    authApi.register.mockReturnValue(of(response));

    service.register(request).subscribe((result) => {
      expect(result).toEqual(response);
    });

    expect(authApi.register).toHaveBeenCalledWith(request);
    expect(session.start).not.toHaveBeenCalled();
  });

  it('should clear the session and navigate to login on logout', () => {
    service.logout();

    expect(session.clear).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
