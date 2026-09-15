import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { AccountFormComponent } from '../../components/account-form/account-form';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account } from '../../models/account.models';
import { AccountEditPageComponent } from './account-edit-page';

describe('AccountEditPageComponent', () => {
  let fixture: ComponentFixture<AccountEditPageComponent>;
  let component: AccountEditPageComponent;
  let accountApi: {
    findById: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '95f90501-5d7b-4b99-bf96-0080639122d0',
    name: 'Conta principal',
    type: 'CHECKING',
    institution: 'Banco Aurora',
    initialBalance: 1200,
    currentBalance: 1380.5,
    status: 'ACTIVE',
    version: 2,
    createdAt: '2026-09-10T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    accountApi = {
      findById: vi.fn().mockReturnValue(of(account)),
      update: vi.fn().mockReturnValue(of(account)),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AccountEditPageComponent],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: ToastService, useValue: toast },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: account.id }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(AccountEditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function accountForm(): AccountFormComponent {
    return fixture.debugElement.query(By.directive(AccountFormComponent))
      .componentInstance as AccountFormComponent;
  }

  it('should load the account from the route id and render edit mode', () => {
    createPage();

    expect(accountApi.findById).toHaveBeenCalledWith(account.id);
    expect(accountForm().mode()).toBe('edit');
    expect(accountForm().account()).toEqual(account);
  });

  it('should render loading while the account request is pending', () => {
    const response = new Subject<Account>();
    accountApi.findById.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    response.next(account);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should show an error state and allow retrying the load', () => {
    accountApi.findById
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(account));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(accountApi.findById).toHaveBeenCalledTimes(2);
    expect(accountForm().account()).toEqual(account);
  });

  it('should send only the changed fields', () => {
    createPage();

    component.updateAccount({
      name: 'Conta de reserva',
      type: account.type,
      institution: account.institution ?? '',
    });

    expect(accountApi.update).toHaveBeenCalledWith(account.id, {
      name: 'Conta de reserva',
    });
  });

  it('should not send a patch when no field changed', () => {
    createPage();

    component.updateAccount({
      name: account.name,
      type: account.type,
      institution: account.institution ?? '',
    });

    expect(accountApi.update).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'info',
      title: 'Nenhuma alteração',
      message: 'Altere pelo menos um campo antes de salvar.',
    });
  });

  it('should show success feedback and navigate to details after updating', () => {
    const updatedAccount = { ...account, name: 'Conta de reserva' };
    accountApi.update.mockReturnValue(of(updatedAccount));

    createPage();
    component.updateAccount({
      name: updatedAccount.name,
      type: account.type,
      institution: account.institution ?? '',
    });

    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta atualizada',
      message: 'As alterações foram salvas com sucesso.',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/accounts', account.id]);
  });

  it('should show reload action on conflict without retrying the patch', () => {
    const conflict = new ApiRequestError({
      timestamp: '2026-09-15T12:00:00Z',
      status: 409,
      code: 'ACCOUNT_CONFLICT',
      message: 'A conta foi alterada.',
      path: `/api/v1/accounts/${account.id}`,
      fieldErrors: [],
    });
    accountApi.update.mockReturnValue(throwError(() => conflict));

    createPage();
    component.updateAccount({
      name: 'Conta de reserva',
      type: account.type,
      institution: account.institution ?? '',
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Dados desatualizados');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(accountApi.update).toHaveBeenCalledTimes(1);

    const reloadButton = fixture.nativeElement.querySelector(
      'app-button button',
    ) as HTMLButtonElement;
    reloadButton.click();
    fixture.detectChanges();

    expect(accountApi.findById).toHaveBeenCalledTimes(2);
    expect(accountApi.update).toHaveBeenCalledTimes(1);
  });
});
