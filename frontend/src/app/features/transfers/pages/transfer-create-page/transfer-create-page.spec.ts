import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { Transaction } from '../../../transactions/models/transaction.models';
import { TransferApiService } from '../../data-access/transfer-api.service';
import { TransferCreatePage } from './transfer-create-page';

describe('TransferCreatePage', () => {
  let fixture: ComponentFixture<TransferCreatePage>;
  let component: TransferCreatePage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let router: Router;
  let toast: { show: ReturnType<typeof vi.fn> };
  let transferApi: { create: ReturnType<typeof vi.fn> };

  const sourceAccount: Account = {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1000,
    currentBalance: 1000,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const destinationAccount: Account = {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Conta Jesse',
    type: 'SAVINGS',
    institution: 'Banco Pollos',
    initialBalance: 500,
    currentBalance: 500,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const inactiveAccount: Account = {
    ...sourceAccount,
    id: '33333333-3333-3333-3333-333333333333',
    name: 'Conta Gus',
    status: 'INACTIVE',
  };

  const transferTransaction: Transaction = {
    id: '44444444-4444-4444-4444-444444444444',
    description: 'Reserva do Jesse Pinkman',
    amount: 250,
    competenceDate: '2026-09-15',
    effectiveDate: '2026-09-15',
    dueDate: null,
    type: 'TRANSFER',
    status: 'COMPLETED',
    paymentMethod: 'TRANSFER',
    sourceAccountId: sourceAccount.id,
    destinationAccountId: destinationAccount.id,
    categoryId: null,
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    accountApi = {
      findAll: vi.fn().mockReturnValue(of([inactiveAccount, destinationAccount, sourceAccount])),
    };

    dialog = {
      confirm: vi.fn().mockReturnValue(of(false)),
    };

    transferApi = {
      create: vi.fn().mockReturnValue(of(transferTransaction)),
    };

    toast = {
      show: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [TransferCreatePage],
      providers: [
        provideRouter([]),
        {
          provide: AccountApiService,
          useValue: accountApi,
        },
        {
          provide: AppDialogService,
          useValue: dialog,
        },
        {
          provide: TransferApiService,
          useValue: transferApi,
        },
        {
          provide: ToastService,
          useValue: toast,
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(TransferCreatePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function fillValidForm(): void {
    component.form.patchValue({
      sourceAccountId: sourceAccount.id,
      destinationAccountId: destinationAccount.id,
      amount: 250,
      date: '2026-09-15',
      description: '  Reserva do Jesse Pinkman  ',
    });

    component.onSourceAccountChange();
    component.onDestinationAccountChange();
  }

  it('loads only active accounts in alphabetical order', () => {
    createPage();

    expect(component.accounts()).toEqual([destinationAccount, sourceAccount]);
    expect(fixture.nativeElement.textContent).not.toContain(inactiveAccount.name);
  });

  it('renders loading while accounts are pending', () => {
    const accounts = new Subject<Account[]>();

    accountApi.findAll.mockReturnValue(accounts.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    accounts.next([sourceAccount, destinationAccount]);
    accounts.complete();
  });

  it('shows an error and retries account loading', () => {
    accountApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of([sourceAccount, destinationAccount]));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar as contas');

    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();

    fixture.detectChanges();

    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
    expect(component.accountsState()).toBe('success');
  });

  it('does not open confirmation for an invalid form', () => {
    createPage();

    component.submit();

    expect(dialog.confirm).not.toHaveBeenCalled();
    expect(transferApi.create).not.toHaveBeenCalled();
  });

  it('blocks a transfer between the same account', () => {
    createPage();

    component.form.patchValue({
      sourceAccountId: sourceAccount.id,
      destinationAccountId: sourceAccount.id,
      amount: 250,
      date: '2026-09-15',
      description: 'Transferência inválida',
    });

    component.submit();

    expect(component.form.hasError('sameAccount')).toBe(true);
    expect(dialog.confirm).not.toHaveBeenCalled();
  });

  it('shows source, destination and amount in confirmation', () => {
    createPage();
    fillValidForm();

    component.submit();

    const confirmation = dialog.confirm.mock.calls[0][0];

    expect(confirmation.title).toBe('Confirmar transferência?');
    expect(confirmation.message).toContain(sourceAccount.name);
    expect(confirmation.message).toContain(destinationAccount.name);
    expect(confirmation.message).toContain('R$');
  });

  it('does not call the API when the user declines confirmation', () => {
    createPage();
    fillValidForm();

    component.submit();

    expect(transferApi.create).not.toHaveBeenCalled();
  });

  it('creates the transfer once, refreshes accounts and navigates after confirmation', () => {
    dialog.confirm.mockReturnValue(of(true));

    createPage();
    fillValidForm();

    component.submit();

    expect(transferApi.create).toHaveBeenCalledWith({
      sourceAccountId: sourceAccount.id,
      destinationAccountId: destinationAccount.id,
      amount: 250,
      date: '2026-09-15',
      description: 'Reserva do Jesse Pinkman',
    });
    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
    expect(toast.show).toHaveBeenCalledWith(
      expect.objectContaining({
        tone: 'success',
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/transactions', transferTransaction.id]);
  });

  it('prevents double submit while confirmation is open', () => {
    const confirmation = new Subject<boolean>();

    dialog.confirm.mockReturnValue(confirmation.asObservable());

    createPage();
    fillValidForm();

    component.submit();
    component.submit();

    expect(dialog.confirm).toHaveBeenCalledTimes(1);

    confirmation.next(false);
    confirmation.complete();
  });

  it('returns to the transaction list when cancelled', () => {
    createPage();

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/transactions']);
  });
});
