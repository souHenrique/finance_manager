import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { SessionService } from '../auth/session.service';
import { authChildGuard, authGuard } from './auth.guards';

describe('authentication guards', () => {
  let router: Router;
  let session: {
    hasValidSession: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    session = {
      hasValidSession: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SessionService,
          useValue: session,
        },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('should allow an authenticated user to activate a private route', () => {
    session.hasValidSession.mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/dashboard' } as RouterStateSnapshot),
    );

    expect(result).toBe(true);
  });

  it('should redirect an unauthenticated user to login with the requested URL', () => {
    session.hasValidSession.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/transactions/new' } as RouterStateSnapshot),
    );

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Ftransactions%2Fnew');
  });

  it('should protect child routes after the user is already inside the shell', () => {
    session.hasValidSession.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authChildGuard({} as ActivatedRouteSnapshot, { url: '/accounts' } as RouterStateSnapshot),
    );

    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Faccounts');
  });
});
