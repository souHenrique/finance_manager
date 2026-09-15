import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { User } from '../../../shared/models/user.models';
import { UpdateProfileRequest } from '../models/profile.models';
import { ProfileApiService } from './profile-api.service';

describe('ProfileApiService', () => {
  let service: ProfileApiService;
  let httpMock: HttpTestingController;

  const user: User = {
    id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
    name: 'Jesse Pinkman',
    email: 'jesse.pinkman@example.com',
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProfileApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(ProfileApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve buscar o perfil do usuário autenticado', () => {
    service.getCurrentUser().subscribe((response) => {
      expect(response).toEqual(user);
    });

    const request = httpMock.expectOne('/api/v1/users/me');

    expect(request.request.method).toBe('GET');

    request.flush(user);
  });

  it('deve atualizar o perfil do usuário autenticado', () => {
    const payload: UpdateProfileRequest = {
      name: 'Skyler White',
      email: 'skyler.white@example.com',
    };
    const response: User = {
      ...user,
      ...payload,
    };

    service.updateCurrentUser(payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne('/api/v1/users/me');

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve atualizar somente o nome quando esse for o campo alterado', () => {
    const payload: UpdateProfileRequest = {
      name: 'Skyler White',
    };

    service.updateCurrentUser(payload).subscribe();

    const request = httpMock.expectOne('/api/v1/users/me');

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      name: 'Skyler White',
    });
    expect(request.request.body).not.toHaveProperty('password');

    request.flush({
      ...user,
      name: 'Skyler White',
    });
  });
});
