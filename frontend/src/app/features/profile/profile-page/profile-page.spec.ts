import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { ToastService } from '../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../core/http/api-request-error';
import { User } from '../../../shared/models/user.models';
import { ProfileApiService } from '../data-access/profile-api.service';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let fixture: ComponentFixture<ProfilePage>;
  let profileApi: {
    getCurrentUser: ReturnType<typeof vi.fn>;
    updateCurrentUser: ReturnType<typeof vi.fn>;
  };
  let toast: {
    show: ReturnType<typeof vi.fn>;
  };

  const user: User = {
    id: '2a1fbc5b-cbb9-4879-b0c5-42f034d64261',
    name: 'Camila Souza',
    email: 'camila.souza@example.com',
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(async () => {
    profileApi = {
      getCurrentUser: vi.fn().mockReturnValue(of(user)),
      updateCurrentUser: vi.fn(),
    };

    toast = {
      show: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [
        {
          provide: ProfileApiService,
          useValue: profileApi,
        },
        {
          provide: ToastService,
          useValue: toast,
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(ProfilePage);
    fixture.detectChanges();
  }

  function getInput(id: string): HTMLInputElement {
    return fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  }

  function fillInput(id: string, value: string): void {
    const input = getInput(id);

    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(
      new Event('submit', {
        bubbles: true,
        cancelable: true,
      }),
    );

    fixture.detectChanges();
  }

  it('should load the current profile when the page opens', () => {
    createPage();

    expect(profileApi.getCurrentUser).toHaveBeenCalledOnce();
    expect(getInput('profile-name').value).toBe(user.name);
    expect(getInput('profile-email').value).toBe(user.email);
  });

  it('should display skeletons while the profile is loading', () => {
    const response = new Subject<User>();

    profileApi.getCurrentUser.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(3);

    response.next(user);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should display an error state and retry loading the profile', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 500,
      code: 'INTERNAL_SERVER_ERROR',
      message: 'Não foi possível carregar o perfil.',
      path: '/api/v1/users/me',
      fieldErrors: [],
    });

    profileApi.getCurrentUser
      .mockReturnValueOnce(throwError(() => apiError))
      .mockReturnValueOnce(of(user));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;

    retryButton.click();
    fixture.detectChanges();

    expect(profileApi.getCurrentUser).toHaveBeenCalledTimes(2);
    expect(getInput('profile-name').value).toBe(user.name);
  });

  it('should not submit an invalid form', () => {
    createPage();

    fillInput('profile-name', '');
    submitForm();

    expect(profileApi.updateCurrentUser).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Campo obrigatório.');
  });

  it('should not send a PATCH when no field changed', () => {
    createPage();

    submitForm();

    expect(profileApi.updateCurrentUser).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'info',
      title: 'Nenhuma alteração',
      message: 'Altere o nome ou o e-mail antes de salvar.',
    });
  });

  it('should send only the changed name and show a success toast', () => {
    const updatedUser: User = {
      ...user,
      name: 'Camila Souza Lima',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    profileApi.updateCurrentUser.mockReturnValue(of(updatedUser));

    createPage();

    fillInput('profile-name', 'Camila Souza Lima');
    submitForm();

    expect(profileApi.updateCurrentUser).toHaveBeenCalledWith({
      name: 'Camila Souza Lima',
    });
    expect(getInput('profile-name').value).toBe('Camila Souza Lima');
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Perfil atualizado',
      message: 'Suas informações foram salvas.',
    });
  });

  it('should send only the changed email', () => {
    const updatedUser: User = {
      ...user,
      email: 'camila.lima@example.com',
      updatedAt: '2026-09-15T12:00:00Z',
    };

    profileApi.updateCurrentUser.mockReturnValue(of(updatedUser));

    createPage();

    fillInput('profile-email', 'camila.lima@example.com');
    submitForm();

    expect(profileApi.updateCurrentUser).toHaveBeenCalledWith({
      email: 'camila.lima@example.com',
    });
  });

  it('should show field errors returned by the API', () => {
    const apiError = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 409,
      code: 'EMAIL_ALREADY_EXISTS',
      message: 'Já existe uma conta com este e-mail.',
      path: '/api/v1/users/me',
      fieldErrors: [
        {
          field: 'email',
          message: 'E-mail já está em uso.',
        },
      ],
    });

    profileApi.updateCurrentUser.mockReturnValue(throwError(() => apiError));

    createPage();

    fillInput('profile-email', 'outro.usuario@example.com');
    submitForm();

    expect(fixture.nativeElement.textContent).toContain('E-mail já está em uso.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('should not expose a password field', () => {
    createPage();

    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
  });
});
