import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Dashboard } from '../models/dashboard.models';
import { DashboardApiService } from './dashboard-api.service';

describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(DashboardApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve buscar os indicadores do dashboard', () => {
    const response: Dashboard = {
      referenceDate: '2026-09-10',
      year: 2026,
      month: 9,
      periodStart: '2026-09-01',
      periodEnd: '2026-09-30',
      monthlyBalance: {
        basis: 'CASH_AND_INVOICE',
        amount: 2750,
      },
      monthlyInflows: {
        basis: 'CASH',
        amount: 5000,
      },
      totalOutflows: {
        basis: 'CASH',
        amount: 18000,
      },
      monthlyOutflows: {
        basis: 'CASH',
        amount: 1500,
      },
      creditCardPurchaseOutflows: {
        basis: 'COMPETENCE',
        amount: 750,
      },
      competenceExpenses: {
        basis: 'COMPETENCE',
        amount: 1800,
      },
      openInvoices: {
        basis: 'COMPETENCE',
        amount: 850,
      },
      monthlyOpenInvoices: {
        basis: 'COMPETENCE',
        amount: 650,
      },
      budget: {
        basis: 'COMPETENCE',
        totalLimit: 3000,
        totalSpent: 2100,
        usagePercentage: 70,
        items: [
          {
            budgetId: 'c487c4cf-d948-4ba8-a85f-e36bb798c928',
            categoryId: '57b1879c-a98e-4718-b66d-47f970ab6709',
            amountLimit: 1500,
            spentAmount: 1200,
            usagePercentage: 80,
            alertStatus: 'ALERT',
          },
        ],
      },
      consolidatedBalance: {
        basis: 'CASH',
        amount: 7000,
      },
    };

    service.get().subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne('/api/v1/dashboard');

    expect(request.request.method).toBe('GET');

    request.flush(response);
  });
});
