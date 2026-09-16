import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { CreditCardFormComponent } from '../../components/credit-card-form/credit-card-form';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreditCard } from '../../models/credit-card.models';
import { CreditCardEditPage } from './credit-card-edit-page';

describe('CreditCardEditPage', () => {
  let fixture: ComponentFixture<CreditCardEditPage>;
  let component: CreditCardEditPage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let creditCardApi: {
    findById: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const account: Account = {
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
  };

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: account.id,
    status: 'ACTIVE',
    version: 2,
  };

  beforeEach(async () => {
    accountApi = { findAll: vi.fn().mockReturnValue(of([account])) };
    creditCardApi = {
      findById: vi.fn().mockReturnValue(of(creditCard)),
      update: vi.fn().mockReturnValue(of(creditCard)),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreditCardEditPage],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: creditCard.id }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(CreditCardEditPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function creditCardForm(): CreditCardFormComponent {
    return fixture.debugElement.query(By.directive(CreditCardFormComponent))
      .componentInstance as CreditCardFormComponent;
  }

  it('should load the card from the route and render the form in edit mode', () => {
    createPage();

    expect(creditCardApi.findById).toHaveBeenCalledWith(creditCard.id);
    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(creditCardForm().mode()).toBe('edit');
    expect(creditCardForm().creditCard()).toEqual(creditCard);
    expect(creditCardForm().accounts()).toEqual([account]);
  });

  it('should render loading until the card and accounts are available', () => {
    const creditCardResponse = new Subject<CreditCard>();
    const accountResponse = new Subject<Account[]>();
    creditCardApi.findById.mockReturnValue(creditCardResponse.asObservable());
    accountApi.findAll.mockReturnValue(accountResponse.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    creditCardResponse.next(creditCard);
    creditCardResponse.complete();
    accountResponse.next([account]);
    accountResponse.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
    expect(creditCardForm().creditCard()).toEqual(creditCard);
  });

  it('should show an error and allow retrying data loading', () => {
    creditCardApi.findById
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(creditCard));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
    expect(creditCardForm().creditCard()).toEqual(creditCard);
  });

  it('should send only fields changed by the user', () => {
    createPage();

    component.updateCreditCard({
      name: 'Cartão Saul',
      creditLimit: creditCard.creditLimit,
      closingDay: creditCard.closingDay,
      dueDay: creditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    });

    expect(creditCardApi.update).toHaveBeenCalledWith(creditCard.id, {
      name: 'Cartão Saul',
    });
  });

  it('should not send a patch when nothing has changed', () => {
    createPage();

    component.updateCreditCard({
      name: creditCard.name,
      creditLimit: creditCard.creditLimit,
      closingDay: creditCard.closingDay,
      dueDay: creditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    });

    expect(creditCardApi.update).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'info',
      title: 'Nenhuma alteração',
      message: 'Altere pelo menos um campo antes de salvar.',
    });
  });

  it('should show feedback and navigate to details after a successful update', () => {
    const updatedCreditCard = { ...creditCard, dueDay: 20 };
    creditCardApi.update.mockReturnValue(of(updatedCreditCard));

    createPage();
    component.updateCreditCard({
      name: creditCard.name,
      creditLimit: creditCard.creditLimit,
      closingDay: creditCard.closingDay,
      dueDay: updatedCreditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    });

    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Cartão atualizado',
      message: 'As alterações foram salvas com sucesso.',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/credit-cards', creditCard.id]);
  });

  it('should show reload data on a limit conflict without retrying the patch', () => {
    const conflict = new ApiRequestError({
      timestamp: '2026-09-16T12:00:00Z',
      status: 409,
      code: 'CREDIT_LIMIT_CONFLICT',
      message: 'O limite informado é menor que o limite comprometido.',
      path: `/api/v1/credit-cards/${creditCard.id}`,
      fieldErrors: [],
    });
    creditCardApi.update.mockReturnValue(throwError(() => conflict));

    createPage();
    component.updateCreditCard({
      name: creditCard.name,
      creditLimit: 3000,
      closingDay: creditCard.closingDay,
      dueDay: creditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível atualizar o limite');
    expect(fixture.nativeElement.textContent).toContain('Recarregar dados');
    expect(creditCardApi.update).toHaveBeenCalledTimes(1);

    const reloadButton = fixture.nativeElement.querySelector(
      '.credit-card-edit__conflict app-button button',
    ) as HTMLButtonElement;
    reloadButton.click();
    fixture.detectChanges();

    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
    expect(creditCardApi.update).toHaveBeenCalledTimes(1);
  });

  it('should return to the card details when the form is cancelled', () => {
    createPage();
    creditCardForm().cancelled.emit();

    expect(router.navigate).toHaveBeenCalledWith(['/credit-cards', creditCard.id]);
  });
});
