import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { PageResponse } from '../../../../shared/models/pagination';
import { CreditCardApiService } from '../../../credit-cards/data-access/credit-card-api.service';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { InvoiceApiService } from '../../data-access/invoice-api.service';
import { InvoiceFilters, InvoiceSummary } from '../../models/invoice.models';
import { InvoiceListPage } from './invoice-list-page';

describe('InvoiceListPage', () => {
  let fixture: ComponentFixture<InvoiceListPage>;
  let component: InvoiceListPage;
  let creditCardApi: { findAll: ReturnType<typeof vi.fn> };
  let invoiceApi: { findAll: ReturnType<typeof vi.fn> };
  let router: Router;
  let activatedRoute: {
    snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> };
  };

  const activeCreditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    status: 'ACTIVE',
    version: 2,
  };

  const inactiveCreditCard: CreditCard = {
    ...activeCreditCard,
    id: 'a63330b4-5742-4e7e-9e4f-547b4df7246d',
    name: 'Cartão Saul',
    status: 'INACTIVE',
  };

  const invoice: InvoiceSummary = {
    id: '72486234-ef50-4c7e-99a7-9193a28533a8',
    creditCardId: activeCreditCard.id,
    referenceMonth: 9,
    referenceYear: 2026,
    closingDate: '2026-09-20',
    dueDate: '2026-09-28',
    totalAmount: 850.75,
    status: 'OPEN',
    paidAt: null,
    version: 0,
  };

  const page: PageResponse<InvoiceSummary> = {
    content: [invoice],
    page: 0,
    size: 20,
    totalElements: 21,
    totalPages: 2,
    first: true,
    last: false,
  };

  beforeEach(async () => {
    creditCardApi = {
      findAll: vi.fn().mockReturnValue(of([inactiveCreditCard, activeCreditCard])),
    };
    invoiceApi = {
      findAll: vi.fn().mockReturnValue(of(page)),
    };
    activatedRoute = {
      snapshot: {
        queryParamMap: convertToParamMap({}),
      },
    };

    await TestBed.configureTestingModule({
      imports: [InvoiceListPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: InvoiceApiService, useValue: invoiceApi },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function createPage(): void {
    fixture = TestBed.createComponent(InvoiceListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function lastQuery(): unknown {
    return invoiceApi.findAll.mock.calls.at(-1)?.[0];
  }

  it('should load cards and invoices with the default descending reference sort', () => {
    createPage();

    expect(creditCardApi.findAll).toHaveBeenCalledOnce();
    expect(invoiceApi.findAll).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      sort: ['referenceYear,desc', 'referenceMonth,desc'],
    });
    expect(component.creditCards()).toEqual([activeCreditCard, inactiveCreditCard]);
    expect(fixture.nativeElement.textContent).toContain('Cartão Heisenberg');
    expect(fixture.nativeElement.textContent).toContain('09/2026');
    expect(fixture.nativeElement.textContent).toContain('Aberta');
  });

  it('should apply the credit card from the URL as the initial filter', () => {
    activatedRoute.snapshot.queryParamMap = convertToParamMap({
      creditCardId: activeCreditCard.id,
    });

    createPage();

    expect(invoiceApi.findAll).toHaveBeenCalledWith(
      expect.objectContaining({ creditCardId: activeCreditCard.id }),
    );
  });

  it('should apply combined filters and reset the visible page', () => {
    createPage();
    component.changePage(2);

    const filters: InvoiceFilters = {
      creditCardId: activeCreditCard.id,
      referenceMonth: 9,
      referenceYear: 2026,
      status: 'CLOSED',
    };
    component.applyFilters(filters);

    expect(component.page()).toBe(0);
    expect(lastQuery()).toEqual({
      ...filters,
      page: 0,
      size: 20,
      sort: ['referenceYear,desc', 'referenceMonth,desc'],
    });
  });

  it('should preserve active filters while changing pages', () => {
    createPage();
    const filters: InvoiceFilters = {
      creditCardId: activeCreditCard.id,
      status: 'PAID',
    };

    component.applyFilters(filters);
    component.changePage(2);

    expect(lastQuery()).toEqual({
      ...filters,
      page: 1,
      size: 20,
      sort: ['referenceYear,desc', 'referenceMonth,desc'],
    });
  });

  it('should clear filters and load the first page again', () => {
    createPage();
    component.applyFilters({ status: 'CLOSED' });
    component.changePage(2);

    component.clearFilters();

    expect(component.filters()).toEqual({});
    expect(component.page()).toBe(0);
    expect(lastQuery()).toEqual({
      page: 0,
      size: 20,
      sort: ['referenceYear,desc', 'referenceMonth,desc'],
    });
  });

  it('should render loading while invoices are pending', () => {
    const invoices = new Subject<PageResponse<InvoiceSummary>>();
    invoiceApi.findAll.mockReturnValue(invoices.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    invoices.next(page);
    invoices.complete();
  });

  it('should render an error and retry invoice loading', () => {
    invoiceApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(page));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar as faturas');

    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(invoiceApi.findAll).toHaveBeenCalledTimes(2);
    expect(component.state()).toBe('success');
  });

  it('should render the empty state when no invoice matches the filters', () => {
    invoiceApi.findAll.mockReturnValue(
      of({
        ...page,
        content: [],
        totalElements: 0,
        totalPages: 0,
        last: true,
      }),
    );

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Nenhuma fatura encontrada');
  });
});
