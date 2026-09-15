import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '../models/budget.models';
import { BudgetApiService } from './budget-api.service';

describe('BudgetApiService', () => {
  let service: BudgetApiService;
  let httpMock: HttpTestingController;

  const budget: Budget = {
    id: 'c487c4cf-d948-4ba8-a85f-e36bb798c928',
    categoryId: '57b1879c-a98e-4718-b66d-47f970ab6709',
    month: 9,
    year: 2026,
    amountLimit: 1500,
    spentAmount: 1200,
    usagePercentage: 80,
    alertStatus: 'ALERT',
    createdAt: '2026-09-08T14:00:00Z',
    updatedAt: '2026-09-08T14:30:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BudgetApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(BudgetApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar um orçamento', () => {
    const payload: CreateBudgetRequest = {
      categoryId: budget.categoryId,
      month: budget.month,
      year: budget.year,
      amountLimit: budget.amountLimit,
    };

    service.create(payload).subscribe((response) => {
      expect(response).toEqual(budget);
    });

    const request = httpMock.expectOne('/api/v1/budgets');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(budget);
  });

  it('deve listar os orçamentos', () => {
    service.findAll().subscribe((response) => {
      expect(response).toEqual([budget]);
    });

    const request = httpMock.expectOne('/api/v1/budgets');

    expect(request.request.method).toBe('GET');

    request.flush([budget]);
  });

  it('deve buscar um orçamento pelo id', () => {
    service.findById(budget.id).subscribe((response) => {
      expect(response).toEqual(budget);
    });

    const request = httpMock.expectOne(`/api/v1/budgets/${budget.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(budget);
  });

  it('deve atualizar um orçamento', () => {
    const payload: UpdateBudgetRequest = {
      month: 10,
      amountLimit: 1800,
    };
    const response: Budget = {
      ...budget,
      ...payload,
    };

    service.update(budget.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/budgets/${budget.id}`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve excluir um orçamento aceitando 204 sem corpo', () => {
    let completed = false;

    service.delete(budget.id).subscribe({
      next: (response) => {
        expect(response).toBeNull();
      },
      complete: () => {
        completed = true;
      },
    });

    const request = httpMock.expectOne(`/api/v1/budgets/${budget.id}`);

    expect(request.request.method).toBe('DELETE');

    request.flush(null, {
      status: 204,
      statusText: 'No Content',
    });

    expect(completed).toBe(true);
  });
});
