import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { CreditCardFormComponent } from '../../components/credit-card-form/credit-card-form';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreditCard, CreateCreditCardRequest } from '../../models/credit-card.models';
import { CreditCardCreatePage } from './credit-card-create-page';

describe('CreditCardCreatePage', () => {
  let fixture: ComponentFixture<CreditCardCreatePage>;
  let component: CreditCardCreatePage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let creditCardApi: { create: ReturnType<typeof vi.fn> };
  let router: Router;
  let toast: { show: ReturnType<typeof vi.fn> };

  const accounts: Account[] = [
    {
      id: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
      name: 'Conta Walter',
      type: 'CHECKING',
      institution: 'Banco Albuquerque',
      initialBalance: 1500,
      currentBalance: 1800,
      status: 'ACTIVE',
      version: 1,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: '49c3a30a-7df5-4602-b25e-11fcd2e9d4e0',
      name: 'Conta Jesse',
      type: 'SAVINGS',
      institution: 'Banco Pinkman',
      initialBalance: 500,
      currentBalance: 700,
      status: 'INACTIVE',
      version: 1,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
  ];

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 5000,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: accounts[0].id,
    status: 'ACTIVE',
    version: 0,
  };

  const request: CreateCreditCardRequest = {
    name: creditCard.name,
    creditLimit: creditCard.creditLimit,
    closingDay: creditCard.closingDay,
    dueDay: creditCard.dueDay,
    defaultAccountId: creditCard.defaultAccountId,
  };

  beforeEach(async () => {
    accountApi = { findAll: vi.fn().mockReturnValue(of(accounts)) };
    creditCardApi = { create: vi.fn().mockReturnValue(of(creditCard)) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreditCardCreatePage],
      providers: [
        provideRouter([]),
        { provide: AccountApiService, useValue: accountApi },
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(CreditCardCreatePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function creditCardForm(): CreditCardFormComponent {
    return fixture.debugElement.query(By.directive(CreditCardFormComponent))
      .componentInstance as CreditCardFormComponent;
  }

  it('should load accounts and render the form in create mode', () => {
    createPage();

    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(creditCardForm().mode()).toBe('create');
    expect(creditCardForm().accounts()).toEqual(accounts);
    expect(creditCardForm().activeAccounts()).toEqual([accounts[0]]);
  });

  it('should render loading while accounts are pending', () => {
    const response = new Subject<Account[]>();
    accountApi.findAll.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(4);

    response.next(accounts);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-credit-card-form')).not.toBeNull();
  });

  it('should show an error and allow retrying account loading', () => {
    accountApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(accounts));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
    expect(creditCardForm().accounts()).toEqual(accounts);
  });

  it('should guide the user when no active account is available', () => {
    accountApi.findAll.mockReturnValue(of([accounts[1]]));

    createPage();

    expect(fixture.nativeElement.querySelector('app-credit-card-form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma conta ativa disponível');
  });

  it('should create a credit card from the submitted request', () => {
    createPage();

    component.createCreditCard(request);

    expect(creditCardApi.create).toHaveBeenCalledWith(request);
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Cartão criado',
      message: 'O cartão de crédito foi criado com sucesso.',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/credit-cards', creditCard.id]);
  });

  it('should disable the form while creation is pending', () => {
    const response = new Subject<CreditCard>();
    creditCardApi.create.mockReturnValue(response.asObservable());

    createPage();
    component.createCreditCard(request);
    fixture.detectChanges();

    expect(component.isSubmitting()).toBe(true);
    expect(creditCardForm().submitting()).toBe(true);

    response.next(creditCard);
    response.complete();
    fixture.detectChanges();

    expect(component.isSubmitting()).toBe(false);
  });

  it('should return to the card list when the form is cancelled', () => {
    createPage();
    creditCardForm().cancelled.emit();

    expect(router.navigate).toHaveBeenCalledWith(['/credit-cards']);
  });
});
