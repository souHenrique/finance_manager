import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountFormComponent } from '../../components/account-form/account-form';
import { AccountApiService } from '../../data-access/account-api.service';
import { Account, CreateAccountRequest } from '../../models/account.models';
import { AccountCreatePageComponent } from './account-create-page';

describe('AccountCreatePageComponent', () => {
  let fixture: ComponentFixture<AccountCreatePageComponent>;
  let component: AccountCreatePageComponent;
  let accountApi: { create: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '281245c7-610b-4ce1-a7dd-7637d1743f10',
    name: 'Conta principal',
    type: 'CHECKING',
    institution: 'Banco Aurora',
    initialBalance: 1200,
    currentBalance: 1200,
    status: 'ACTIVE',
    version: 0,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const request: CreateAccountRequest = {
    name: 'Conta principal',
    type: 'CHECKING',
    institution: 'Banco Aurora',
    initialBalance: 1200,
  };

  beforeEach(async () => {
    accountApi = { create: vi.fn().mockReturnValue(of(account)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AccountCreatePageComponent],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: ToastService, useValue: toast },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountCreatePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function accountForm(): AccountFormComponent {
    return fixture.debugElement.query(By.directive(AccountFormComponent))
      .componentInstance as AccountFormComponent;
  }

  it('should render the reusable form in create mode', () => {
    expect(accountForm().mode()).toBe('create');
  });

  it('should create an account from the submitted request', () => {
    component.createAccount(request);

    expect(accountApi.create).toHaveBeenCalledWith(request);
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Conta criada',
      message: 'A conta foi criada com sucesso.',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/accounts', account.id]);
  });

  it('should disable the form while creation is pending', () => {
    const response = new Subject<Account>();
    accountApi.create.mockReturnValue(response.asObservable());

    component.createAccount(request);
    fixture.detectChanges();

    expect(component.isSubmitting()).toBe(true);
    expect(accountForm().submitting()).toBe(true);

    response.next(account);
    response.complete();
    fixture.detectChanges();

    expect(component.isSubmitting()).toBe(false);
  });

  it('should return to the account list when the form is cancelled', () => {
    accountForm().cancelled.emit();

    expect(router.navigate).toHaveBeenCalledWith(['/accounts']);
  });
});
