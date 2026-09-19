import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { ApiRequestError } from '../../../core/http/api-request-error';
import { AuthService } from '../services/auth.service';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let harness: RouterTestingHarness;
  let router: Router;
  let auth: { login: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = { login: vi.fn() };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'login',
            component: LoginPage,
          },
        ]),
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  async function openLoginPage(url = '/login'): Promise<LoginPage> {
    harness = await RouterTestingHarness.create();

    return harness.navigateByUrl(url, LoginPage);
  }

  function fillInput(id: string, value: string): void {
    const input = harness.routeNativeElement?.querySelector(`#${id}`) as HTMLInputElement;

    input.value = value;
    input.dispatchEvent(new Event('input'));
    harness.detectChanges();
  }

  function submitForm(): void {
    const form = harness.routeNativeElement?.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(
      new Event('submit', {
        bubbles: true,
        cancelable: true,
      }),
    );
    harness.detectChanges();
  }

  it('should not call the API when required fields are invalid', async () => {
    await openLoginPage();

    submitForm();

    expect(auth.login).not.toHaveBeenCalled();
    expect(harness.routeNativeElement?.textContent).toContain('Campo obrigatório.');
  });

  it('should submit the credentials and navigate to dashboard by default', async () => {
    await openLoginPage();
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    auth.login.mockReturnValue(
      of({
        expiresIn: 3600,
      }),
    );

    fillInput('login-email', 'jesse.pinkman@example.com');
    fillInput('login-password', 'SenhaSegura123');
    submitForm();

    expect(auth.login).toHaveBeenCalledWith({
      email: 'jesse.pinkman@example.com',
      password: 'SenhaSegura123',
    });
    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should return the user to a safe requested private route', async () => {
    await openLoginPage('/login?returnUrl=%2Ftransactions%2Fnew');
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    auth.login.mockReturnValue(
      of({
        expiresIn: 3600,
      }),
    );

    fillInput('login-email', 'jesse.pinkman@example.com');
    fillInput('login-password', 'SenhaSegura123');
    submitForm();

    expect(navigateByUrl).toHaveBeenCalledWith('/transactions/new');
  });

  it('should reject an external return URL', async () => {
    await openLoginPage('/login?returnUrl=%2F%2Fevil.example');
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    auth.login.mockReturnValue(
      of({
        expiresIn: 3600,
      }),
    );

    fillInput('login-email', 'jesse.pinkman@example.com');
    fillInput('login-password', 'SenhaSegura123');
    submitForm();

    expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should show field errors returned by the API', async () => {
    await openLoginPage();
    auth.login.mockReturnValue(
      throwError(
        () =>
          new ApiRequestError({
            timestamp: '2026-09-15T12:00:00Z',
            status: 400,
            code: 'VALIDATION_ERROR',
            message: 'Verifique os dados informados.',
            path: '/api/v1/auth/login',
            fieldErrors: [
              {
                field: 'email',
                message: 'E-mail inválido.',
              },
            ],
          }),
      ),
    );

    fillInput('login-email', 'jesse.pinkman@example.com');
    fillInput('login-password', 'SenhaSegura123');
    submitForm();

    expect(harness.routeNativeElement?.textContent).toContain('E-mail inválido.');
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')).not.toBeNull();
  });
});
