import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from './app.routes';
import { AccountApiService } from './features/accounts/data-access/account-api.service';

describe('Application routes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: AccountApiService,
          useValue: {
            findAll: () => of([]),
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
