import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { PageResponse } from '../../../shared/models/pagination';
import {
  CreateTransactionRequest,
  Transaction,
  UpdateTransactionRequest,
} from '../models/transaction.models';
import { TransactionApiService } from './transaction-api.service';

describe('TransactionApiService', () => {
  let service: TransactionApiService;
  let httpMock: HttpTestingController;

  const transaction: Transaction = {
    id: '2cb0ba91-bfc4-43be-89ec-336ca64a6231',
    description: 'Compra no supermercado',
    amount: 180.5,
    competenceDate: '2026-09-02',
    effectiveDate: '2026-09-02',
    dueDate: null,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    destinationAccountId: null,
    categoryId: '57b1879c-a98e-4718-b66d-47f970ab6709',
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransactionApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(TransactionApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar uma transação', () => {
    const payload: CreateTransactionRequest = {
      description: transaction.description,
      amount: transaction.amount,
      competenceDate: transaction.competenceDate,
      effectiveDate: transaction.effectiveDate,
      type: 'EXPENSE',
      status: 'COMPLETED',
      paymentMethod: 'PIX',
      sourceAccountId: transaction.sourceAccountId,
      categoryId: transaction.categoryId!,
    };

    service.create(payload).subscribe((response) => {
      expect(response).toEqual(transaction);
    });

    const request = httpMock.expectOne('/api/v1/transactions');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(transaction);
  });

  it('deve listar transações com filtros e paginação', () => {
    const page: PageResponse<Transaction> = {
      content: [transaction],
      page: 2,
      size: 10,
      totalElements: 21,
      totalPages: 3,
      first: false,
      last: true,
    };

    service
      .findAll({
        startDate: '2026-09-01',
        endDate: '2026-09-30',
        categoryId: transaction.categoryId!,
        accountId: transaction.sourceAccountId!,
        creditCardId: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
        type: 'EXPENSE',
        status: 'COMPLETED',
        minAmount: 50,
        maxAmount: 500,
        description: 'mercado',
        page: 2,
        size: 10,
        sort: ['competenceDate,desc', 'createdAt,desc'],
      })
      .subscribe((response) => {
        expect(response).toEqual(page);
      });

    const request = httpMock.expectOne((candidate) => candidate.url === '/api/v1/transactions');

    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('startDate')).toBe('2026-09-01');
    expect(request.request.params.get('endDate')).toBe('2026-09-30');
    expect(request.request.params.get('categoryId')).toBe(transaction.categoryId);
    expect(request.request.params.get('accountId')).toBe(transaction.sourceAccountId);
    expect(request.request.params.get('creditCardId')).toBe('d89835ee-3463-4a35-a2e9-38d96ab17418');
    expect(request.request.params.get('type')).toBe('EXPENSE');
    expect(request.request.params.get('status')).toBe('COMPLETED');
    expect(request.request.params.get('minAmount')).toBe('50');
    expect(request.request.params.get('maxAmount')).toBe('500');
    expect(request.request.params.get('description')).toBe('mercado');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.getAll('sort')).toEqual([
      'competenceDate,desc',
      'createdAt,desc',
    ]);

    request.flush(page);
  });

  it('deve buscar uma transação pelo id', () => {
    service.findById(transaction.id).subscribe((response) => {
      expect(response).toEqual(transaction);
    });

    const request = httpMock.expectOne(`/api/v1/transactions/${transaction.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(transaction);
  });

  it('deve atualizar uma transação', () => {
    const payload: UpdateTransactionRequest = {
      description: 'Compra mensal no supermercado',
      amount: 210.9,
      competenceDate: '2026-09-03',
    };
    const response: Transaction = {
      ...transaction,
      ...payload,
    };

    service.update(transaction.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/transactions/${transaction.id}`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve cancelar uma transação sem enviar payload financeiro', () => {
    const response: Transaction = {
      ...transaction,
      status: 'CANCELLED',
    };

    service.cancel(transaction.id).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/transactions/${transaction.id}/cancel`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();

    request.flush(response);
  });
});
