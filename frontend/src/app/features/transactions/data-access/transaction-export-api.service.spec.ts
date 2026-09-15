import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { TransactionExportApiService } from './transaction-export-api.service';

describe('TransactionExportApiService', () => {
  let service: TransactionExportApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransactionExportApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(TransactionExportApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve baixar o CSV reutilizando os filtros de transações', () => {
    const csv = new Blob(['id,description\n1,Café'], { type: 'text/csv;charset=UTF-8' });

    service
      .download({
        startDate: '2026-09-01',
        endDate: '2026-09-30',
        type: 'EXPENSE',
        status: 'COMPLETED',
        description: 'café',
      })
      .subscribe((response) => {
        expect(response).toBe(csv);
      });

    const request = httpMock.expectOne(
      (candidate) => candidate.url === '/api/v1/exports/transactions.csv',
    );

    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.params.get('startDate')).toBe('2026-09-01');
    expect(request.request.params.get('endDate')).toBe('2026-09-30');
    expect(request.request.params.get('type')).toBe('EXPENSE');
    expect(request.request.params.get('status')).toBe('COMPLETED');
    expect(request.request.params.get('description')).toBe('café');
    expect(request.request.params.has('page')).toBe(false);
    expect(request.request.params.has('size')).toBe(false);

    request.flush(csv);
  });
});
