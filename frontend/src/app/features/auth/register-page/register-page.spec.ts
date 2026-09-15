import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { AuthService } from '../services/auth.service';
import { RegisterPage } from './register-page';

describe('RegisterPage', () => {
  let harness: RouterTestingHarness;
  let router: Router;
  let auth: { register: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    auth = { register: vi.fn() };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'register',
            component: RegisterPage,
          },
        ]),
        { provide: AuthService, useValue: auth },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  async function openRegisterPage(): Promise<RegisterPage> {
    harness = await RouterTestingHarness.create();

    return harness.navigateByUrl('/register', RegisterPage);
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

  function fillValidForm(): void {
    fillInput('register-name', ' Henrique Amorim ');
    fillInput('register-email', 'henrique@example.com');
    fillInput('register-password', 'SenhaSegura123');
    fillInput('register-confirm-password', 'SenhaSegura123');
  }

  it('should not submit invalid required fields', async () => {
    await openRegisterPage();

    submitForm();

    expect(auth.register).not.toHaveBeenCalled();
    expect(harness.routeNativeElement?.textContent).toContain('Campo obrigatório.');
  });

  it('should prevent registration when the passwords differ', async () => {
    await openRegisterPage();
    fillInput('register-name', 'Henrique Amorim');
    fillInput('register-email', 'henrique@example.com');
    fillInput('register-password', 'SenhaSegura123');
    fillInput('register-confirm-password', 'SenhaDiferente123');

    submitForm();

    expect(auth.register).not.toHaveBeenCalled();
    expect(harness.routeNativeElement?.textContent).toContain('As senhas não coincidem.');
  });

  it('should submit only the API registration contract and redirect to login', async () => {
    await openRegisterPage();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    auth.register.mockReturnValue(
      of({
        id: 'f02e76b3-8d53-42dd-b4c5-43fcda2d3d84',
        name: 'Henrique Amorim',
        email: 'henrique@example.com',
        createdAt: '2026-09-15T12:00:00Z',
        updatedAt: '2026-09-15T12:00:00Z',
      }),
    );

    fillValidForm();
    submitForm();

    expect(auth.register).toHaveBeenCalledWith({
      name: 'Henrique Amorim',
      email: 'henrique@example.com',
      password: 'SenhaSegura123',
    });
    expect(auth.register.mock.calls[0]?.[0]).not.toHaveProperty('confirmPassword');
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta criada',
      message: 'Agora você já pode entrar.',
    });
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should display field errors returned by the API', async () => {
    await openRegisterPage();
    auth.register.mockReturnValue(
      throwError(
        () =>
          new ApiRequestError({
            timestamp: '2026-09-15T12:00:00Z',
            status: 409,
            code: 'EMAIL_ALREADY_EXISTS',
            message: 'Já existe uma conta com este e-mail.',
            path: '/api/v1/auth/register',
            fieldErrors: [
              {
                field: 'email',
                message: 'E-mail já está em uso.',
              },
            ],
          }),
      ),
    );

    fillValidForm();
    submitForm();

    expect(harness.routeNativeElement?.textContent).toContain('E-mail já está em uso.');
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')).not.toBeNull();
  });
});
