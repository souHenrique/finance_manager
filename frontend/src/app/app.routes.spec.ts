import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { SessionService } from './core/auth/session.service';
import { AccountApiService } from './features/accounts/data-access/account-api.service';
import { AuthService } from './features/auth/services/auth.service';
import { ProfileApiService } from './features/profile/data-access/profile-api.service';
import { CategoryApiService } from './features/categories/data-access/category-api.service';
import { CreditCardApiService } from './features/credit-cards/data-access/credit-card-api.service';
import { TransactionApiService } from './features/transactions/data-access/transaction-api.service';

describe('Application routes', () => {
  let session: { hasValidSession: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    session = {
      hasValidSession: vi.fn().mockReturnValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: SessionService,
          useValue: session,
        },
        {
          provide: AuthService,
          useValue: {
            logout: vi.fn(),
          },
        },
        {
          provide: ProfileApiService,
          useValue: {
            getCurrentUser: () =>
              of({
                id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
                name: 'Jesse Pinkman',
                email: 'jesse.pinkman@example.com',
                createdAt: '2026-09-02T12:00:00Z',
                updatedAt: '2026-09-02T12:30:00Z',
              }),
            updateCurrentUser: vi.fn(),
          },
        },
        {
          provide: AccountApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
            findById: vi.fn().mockReturnValue(
              of({
                id: '123',
                name: 'Conta Walter',
                type: 'CHECKING',
                institution: 'Banco Albuquerque',
                initialBalance: 1000,
                currentBalance: 1000,
                status: 'ACTIVE',
                version: 0,
                createdAt: '2026-09-15T10:00:00Z',
                updatedAt: '2026-09-15T10:00:00Z',
              }),
            ),
          },
        },
        {
          provide: CategoryApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
          },
        },
        {
          provide: CreditCardApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(of([])),
          },
        },
        {
          provide: TransactionApiService,
          useValue: {
            findAll: vi.fn().mockReturnValue(
              of({
                content: [],
                totalElements: 0,
                totalPages: 0,
                size: 20,
                number: 0,
              }),
            ),
          },
        },
      ],
    });
  });

  it.each(['/login', '/register'])(
    'should render %s without the authenticated shell',
    async (url) => {
      const harness = await RouterTestingHarness.create();

      await harness.navigateByUrl(url);

      expect(TestBed.inject(Router).url).toBe(url);
      expect(harness.routeNativeElement?.querySelector('app-header')).toBeNull();
      expect(harness.routeNativeElement?.querySelector('app-sidebar')).toBeNull();
    },
  );

  it('should render authenticated routes inside the shell', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/dashboard');

    expect(harness.routeNativeElement?.querySelector('app-header')).not.toBeNull();
    expect(harness.routeNativeElement?.querySelector('app-sidebar')).not.toBeNull();
  });

  it('should redirect an unauthenticated user away from a private route', async () => {
    session.hasValidSession.mockReturnValue(false);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/dashboard');

    expect(TestBed.inject(Router).url).toBe('/login?returnUrl=%2Fdashboard');
    expect(harness.routeNativeElement?.querySelector('app-header')).toBeNull();
  });

  it.each([
    '/dashboard',
    '/transactions',
    '/transactions/new',
    '/transactions/123',
    '/transfers/new',
    '/accounts',
    '/accounts/123',
    '/categories',
    '/credit-cards',
    '/credit-cards/123',
    '/invoices',
    '/invoices/123',
    '/budgets',
    '/reports',
    '/profile',
  ])('should navigate successfully to %s', async (url) => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl(url);

    expect(TestBed.inject(Router).url).toBe(url);
    expect(harness.routeNativeElement?.textContent).not.toContain('Erro 404');
  });

  it('should render the 404 page for an unknown route', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/rota-inexistente');

    expect(harness.routeNativeElement?.textContent).toContain('Erro 404');
    expect(harness.routeNativeElement?.textContent).toContain('Página não encontrada');
  });
});
