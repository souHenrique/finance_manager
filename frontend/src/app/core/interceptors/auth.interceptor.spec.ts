import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../config/api-base-url';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends API requests with credentials so the HttpOnly cookie is included', () => {
    http.get('/api/v1/accounts').subscribe();
    const request = httpMock.expectOne('/api/v1/accounts');

    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it.each(['/api/v1/auth/login', '/api/v1/auth/register', '/api/v1/auth/logout'])(
    'sends credentials to %s so the backend can set or clear the cookie',
    (url) => {
      http.post(url, {}).subscribe();
      const request = httpMock.expectOne(url);

      expect(request.request.withCredentials).toBe(true);
      request.flush({});
    },
  );

  it('does not send credentials to an external URL', () => {
    http.get('https://example.com/profile').subscribe();
    const request = httpMock.expectOne('https://example.com/profile');

    expect(request.request.withCredentials).toBe(false);
    request.flush({});
  });
});
