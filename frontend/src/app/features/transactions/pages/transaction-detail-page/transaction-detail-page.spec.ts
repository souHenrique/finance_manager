import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import { Transaction } from '../../models/transaction.models';
import { TransactionDetailPage } from './transaction-detail-page';

describe('TransactionDetailPage', () => {
  let fixture: ComponentFixture<TransactionDetailPage>;
  let component: TransactionDetailPage;
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };
  let transactionApi: { cancel: ReturnType<typeof vi.fn>; findById: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: 'ef2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1000,
    currentBalance: 850,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const category: Category = {
    id: 'ff2e3d4c-5b6a-7980-1234-56789abcdef0',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };
  const transaction: Transaction = {
    id: '0a2e3d4c-5b6a-7980-1234-56789abcdef0',
    description: 'Mercado do Jesse Pinkman',
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

  beforeEach(async () => {
    dialog = { confirm: vi.fn().mockReturnValue(of(false)) };
    toast = { show: vi.fn() };
    transactionApi = {
      findById: vi.fn().mockReturnValue(of(transaction)),
      cancel: vi.fn().mockReturnValue(of({ ...transaction, status: 'CANCELLED' })),
    };

    await TestBed.configureTestingModule({
      imports: [TransactionDetailPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: transaction.id }) } },
        },
        { provide: TransactionApiService, useValue: transactionApi },
        {
          provide: AccountApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([account])) },
        },
        {
          provide: CategoryApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([category])) },
        },
        { provide: CreditCardApiService, useValue: { findAll: vi.fn().mockReturnValue(of([])) } },
        { provide: AppDialogService, useValue: dialog },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(TransactionDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads the transaction by id and renders its data and status', () => {
    createPage();

    expect(transactionApi.findById).toHaveBeenCalledWith(transaction.id);
    expect(fixture.nativeElement.textContent).toContain(transaction.description);
    expect(fixture.nativeElement.textContent).toContain('Concluída');
    expect(fixture.nativeElement.textContent).toContain(account.name);
    expect(fixture.nativeElement.textContent).toContain(category.name);
  });

  it('shows edit and cancel actions for an active transaction', () => {
    createPage();

    expect(fixture.nativeElement.textContent).toContain('Editar');
    expect(fixture.nativeElement.textContent).toContain('Cancelar transação');
  });

  it('does not show mutation actions for a cancelled transaction', () => {
    transactionApi.findById.mockReturnValue(of({ ...transaction, status: 'CANCELLED' }));
    createPage();

    expect(fixture.nativeElement.textContent).toContain('Transação cancelada');
    expect(fixture.nativeElement.textContent).not.toContain('Cancelar transação');
    expect(fixture.nativeElement.textContent).not.toContain('Editar');
  });

  it('opens confirmation and does not cancel when the user declines', () => {
    createPage();

    component.confirmCancellation();

    expect(dialog.confirm).toHaveBeenCalledWith(expect.objectContaining({ danger: true }));
    expect(transactionApi.cancel).not.toHaveBeenCalled();
  });

  it('cancels after confirmation, shows feedback and preserves the transaction', () => {
    dialog.confirm.mockReturnValue(of(true));
    createPage();

    component.confirmCancellation();
    fixture.detectChanges();

    expect(transactionApi.cancel).toHaveBeenCalledWith(transaction.id);
    expect(toast.show).toHaveBeenCalledWith(expect.objectContaining({ tone: 'success' }));
    expect(component.transaction()).toEqual(expect.objectContaining({ status: 'CANCELLED' }));
    expect(fixture.nativeElement.textContent).toContain('Transação cancelada');
    expect(fixture.nativeElement.textContent).toContain(transaction.description);
    expect(fixture.nativeElement.textContent).not.toContain('Cancelar transação');
  });
});
