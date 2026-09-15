import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SessionService } from '../auth/session.service';
import { API_BASE_URL } from '../config/api-base-url';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let session: {
    getAuthorizationHeader: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    session = {
      getAuthorizationHeader: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
        {
          provide: SessionService,
          useValue: session,
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add the authorization header to private API requests', () => {
    session.getAuthorizationHeader.mockReturnValue('Bearer jwt-token');

    http.get('/api/v1/accounts').subscribe();

    const request = httpMock.expectOne('/api/v1/accounts');

    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');

    request.flush({});
  });

  it.each(['/api/v1/auth/login', '/api/v1/auth/register'])(
    'should not send a previous token to %s',
    (url) => {
      session.getAuthorizationHeader.mockReturnValue('Bearer jwt-token');

      http.post(url, {}).subscribe();

      const request = httpMock.expectOne(url);

      expect(request.request.headers.has('Authorization')).toBe(false);

      request.flush({});
    },
  );

  it('should not send the token to an external URL', () => {
    session.getAuthorizationHeader.mockReturnValue('Bearer jwt-token');

    http.get('https://example.com/profile').subscribe();

    const request = httpMock.expectOne('https://example.com/profile');

    expect(request.request.headers.has('Authorization')).toBe(false);

    request.flush({});
  });

  it('should not send an authorization header without a valid session', () => {
    session.getAuthorizationHeader.mockReturnValue(null);

    http.get('/api/v1/accounts').subscribe();

    const request = httpMock.expectOne('/api/v1/accounts');

    expect(request.request.headers.has('Authorization')).toBe(false);

    request.flush({});
  });
});
