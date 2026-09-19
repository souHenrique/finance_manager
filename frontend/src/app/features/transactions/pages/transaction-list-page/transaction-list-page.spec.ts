import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { PageResponse } from '../../../../shared/models/pagination';
import { TransactionApiService } from '../../data-access/transaction-api.service';
import { TransactionExportApiService } from '../../data-access/transaction-export-api.service';
import { Transaction, TransactionListItem } from '../../models/transaction.models';
import { TransactionListPage } from './transaction-list-page';

describe('TransactionListPage', () => {
  let fixture: ComponentFixture<TransactionListPage>;
  let component: TransactionListPage;
  let transactionApi: { findAll: ReturnType<typeof vi.fn> };
  let transactionExportApi: { download: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '5a8c2f54-8366-46c9-9b5d-08d55f7c2a7b',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1200,
    currentBalance: 1250,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const category: Category = {
    id: '95f83bdb-867b-45c9-a93b-2c7d9f0eb939',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const creditCard: CreditCard = {
    id: '524f92d5-88e5-4a6d-a83f-ea53e650c5af',
    name: 'Cartão Saul',
    creditLimit: 5000,
    availableLimit: 4300,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: account.id,
    status: 'ACTIVE',
    version: 1,
  };

  const transaction: Transaction = {
    id: '0f3bc18d-8727-4f03-89a0-4b6f40647a6b',
    description: 'Compra para Jesse Pinkman',
    amount: 180.5,
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

  const result: PageResponse<TransactionListItem> = {
    content: [
      {
        transaction,
        displayAmount: transaction.amount,
        installmentPurchase: false,
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
  };

  const csv = new Blob(['id,description\n1,Jesse Pinkman'], {
    type: 'text/csv;charset=UTF-8',
  });

  beforeEach(async () => {
    transactionApi = { findAll: vi.fn().mockReturnValue(of(result)) };
    transactionExportApi = { download: vi.fn().mockReturnValue(of(csv)) };

    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:transactions-csv');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [TransactionListPage],
      providers: [
        provideRouter([]),
        {
          provide: TransactionApiService,
          useValue: transactionApi,
        },
        {
          provide: TransactionExportApiService,
          useValue: transactionExportApi,
        },
        {
          provide: AccountApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([account])) },
        },
        {
          provide: CategoryApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([category])) },
        },
        {
          provide: CreditCardApiService,
          useValue: { findAll: vi.fn().mockReturnValue(of([creditCard])) },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(TransactionListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load the first page with default sorting', () => {
    createPage();

    expect(transactionApi.findAll).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      sort: 'competenceDate,desc',
    });
  });

  it('should apply filters and return to the first API page', () => {
    createPage();
    component.page.set(3);

    component.applyFilters({
      description: 'Jesse',
      status: 'COMPLETED',
    });

    expect(component.page()).toBe(0);
    expect(transactionApi.findAll).toHaveBeenLastCalledWith({
      description: 'Jesse',
      status: 'COMPLETED',
      page: 0,
      size: 20,
      sort: 'competenceDate,desc',
    });
  });

  it('should clear filters and return to the first API page', () => {
    createPage();
    component.filters.set({ description: 'Walter' });
    component.page.set(2);

    component.clearFilters();

    expect(component.filters()).toEqual({});
    expect(component.page()).toBe(0);
    expect(transactionApi.findAll).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      sort: 'competenceDate,desc',
    });
  });

  it('should preserve filters while changing the visible page', () => {
    createPage();
    component.applyFilters({
      description: 'Jesse',
      status: 'COMPLETED',
    });

    component.changePage(2);

    expect(transactionApi.findAll).toHaveBeenLastCalledWith({
      description: 'Jesse',
      status: 'COMPLETED',
      page: 1,
      size: 20,
      sort: 'competenceDate,desc',
    });
  });

  it('should change sorting and return to the first page', () => {
    createPage();
    component.page.set(2);

    component.changeSort('amount,asc');

    expect(component.page()).toBe(0);
    expect(transactionApi.findAll).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      sort: 'amount,asc',
    });
  });

  it('should render skeletons while transactions are loading', () => {
    const response = new Subject<PageResponse<TransactionListItem>>();
    transactionApi.findAll.mockReturnValue(response.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(3);

    response.next(result);
    response.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should show an error state and retry loading transactions', () => {
    transactionApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(result));

    createPage();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(transactionApi.findAll).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
  });

  it('should show an empty state when there are no transactions', () => {
    transactionApi.findAll.mockReturnValue(of({ ...result, content: [], totalElements: 0 }));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma transação encontrada');
  });

  it('should render transaction data with readable reference names', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
    expect(content).toContain(transaction.description);
    expect(content).toContain('Despesa');
    expect(content).toContain('Concluída');
    expect(content).toContain(category.name);
    expect(content).toContain(account.name);
  });

  it('should show one total purchase entry for a credit-card installment group', () => {
    transactionApi.findAll.mockReturnValue(
      of({
        ...result,
        content: [
          {
            transaction: {
              ...transaction,
              amount: 100,
              type: 'CREDIT_CARD_PURCHASE',
              paymentMethod: 'CREDIT_CARD',
              creditCardId: creditCard.id,
              installmentGroupId: 'c9d31f8b-4803-40bb-8b30-0eb4a5467991',
              installmentNumber: 1,
              installmentCount: 3,
            },
            displayAmount: 300,
            installmentPurchase: true,
          },
        ],
      }),
    );

    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(content).toContain('Compra parcelada em 3x');
    expect(content).toContain('R$300.00');
    expect(content).toContain('Ver compra e parcelas');
  });

  it('should export the exact active filters without pagination', () => {
    createPage();
    const filters = {
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      categoryId: category.id,
      accountId: account.id,
      type: 'EXPENSE' as const,
      status: 'COMPLETED' as const,
      minAmount: 50,
      maxAmount: 500,
      description: 'Jesse',
    };
    component.filters.set(filters);

    component.exportCsv();

    expect(transactionExportApi.download).toHaveBeenCalledWith(filters);
    expect(transactionExportApi.download).not.toHaveBeenCalledWith(
      expect.objectContaining({ page: expect.anything() }),
    );
  });

  it('should download the returned blob as transactions.csv', () => {
    const appendChild = vi.spyOn(document.body, 'appendChild');
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click');
    createPage();

    component.exportCsv();

    const link = appendChild.mock.calls.find(
      ([element]) => element instanceof HTMLAnchorElement,
    )?.[0] as HTMLAnchorElement;

    expect(URL.createObjectURL).toHaveBeenCalledWith(csv);
    expect(link.download).toBe('transactions.csv');
    expect(click).toHaveBeenCalledOnce();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:transactions-csv');
    expect(component.isExporting()).toBe(false);
  });

  it('should prevent duplicate exports while the first download is pending', () => {
    const response = new Subject<Blob>();
    transactionExportApi.download.mockReturnValue(response.asObservable());
    createPage();

    component.exportCsv();
    component.exportCsv();

    expect(transactionExportApi.download).toHaveBeenCalledTimes(1);
    expect(component.isExporting()).toBe(true);

    response.next(csv);
    response.complete();

    expect(component.isExporting()).toBe(false);
  });

  it('should re-enable export after an error', () => {
    transactionExportApi.download.mockReturnValue(throwError(() => new Error('network')));
    createPage();

    component.exportCsv();

    expect(component.isExporting()).toBe(false);
  });
});
