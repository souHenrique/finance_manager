import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { TransactionFormComponent } from '../../components/transaction-form/transaction-form';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import { CreateTransactionRequest, Transaction } from '../../models/transaction.models';
import { TransactionCreatePage } from './transaction-create-page';

describe('TransactionCreatePage', () => {
  let fixture: ComponentFixture<TransactionCreatePage>;
  let component: TransactionCreatePage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let categoryApi: { findAll: ReturnType<typeof vi.fn> };
  let router: Router;
  let toast: { show: ReturnType<typeof vi.fn> };
  let transactionApi: { create: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '7f2e3d4c-5b6a-7980-1234-56789abcdef0',
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
  const inactiveAccount: Account = {
    ...account,
    id: '8f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Conta Gus',
    status: 'INACTIVE',
  };
  const category: Category = {
    id: '9f2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const transaction: Transaction = {
    id: 'af2e3d4c-5b6a-7980-1234-56789abcdef0',
    description: 'Compra para Jesse Pinkman',
    amount: 150,
    competenceDate: '2026-09-15',
    effectiveDate: '2026-09-15',
    dueDate: null,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: account.id,
    destinationAccountId: null,
    categoryId: category.id,
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const payload: CreateTransactionRequest = {
    description: transaction.description,
    amount: transaction.amount,
    competenceDate: transaction.competenceDate,
    effectiveDate: transaction.effectiveDate,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: account.id,
    destinationAccountId: null,
    categoryId: category.id,
  };

  beforeEach(async () => {
    accountApi = { findAll: vi.fn().mockReturnValue(of([inactiveAccount, account])) };
    categoryApi = { findAll: vi.fn().mockReturnValue(of([category])) };
    transactionApi = { create: vi.fn().mockReturnValue(of(transaction)) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [TransactionCreatePage],
      providers: [
        provideRouter([]),
        { provide: AccountApiService, useValue: accountApi },
        { provide: CategoryApiService, useValue: categoryApi },
        { provide: TransactionApiService, useValue: transactionApi },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(TransactionCreatePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads active accounts and categories for the form', () => {
    createPage();
    const form = fixture.debugElement.query(By.directive(TransactionFormComponent))
      .componentInstance as TransactionFormComponent;

    expect(component.optionsState()).toBe('success');
    expect(form.mode()).toBe('create');
    expect(component.accounts()).toEqual([account]);
    expect(component.categories()).toEqual([category]);
  });

  it('renders loading while options are pending', () => {
    const accounts = new Subject<Account[]>();
    accountApi.findAll.mockReturnValue(accounts.asObservable());
    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    accounts.next([account]);
    accounts.complete();
  });

  it('shows an error and retries loading options', () => {
    accountApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of([account]));
    createPage();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível preparar o formulário');
    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
    expect(component.optionsState()).toBe('success');
  });

  it('creates a transaction, shows feedback and navigates to detail', () => {
    createPage();

    component.create(payload);

    expect(transactionApi.create).toHaveBeenCalledWith(payload);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ tone: 'success' }));
    expect(router.navigate).toHaveBeenCalledWith(['/transactions', transaction.id]);
  });

  it('returns to the transaction list when cancelled', () => {
    createPage();

    component.goBack();

    expect(router.navigate).toHaveBeenCalledWith(['/transactions']);
  });

  it('keeps the form disabled while creation is pending', () => {
    const response = new Subject<Transaction>();
    transactionApi.create.mockReturnValue(response.asObservable());
    createPage();

    component.create(payload);
    fixture.detectChanges();

    const form = fixture.debugElement.query(By.directive(TransactionFormComponent))
      .componentInstance as TransactionFormComponent;
    expect(form.submitting()).toBe(true);
  });
});
