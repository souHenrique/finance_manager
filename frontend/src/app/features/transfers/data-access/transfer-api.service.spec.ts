import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Transaction } from '../../transactions/models/transaction.models';
import { CreateTransferRequest } from '../models/transfer.models';
import { TransferApiService } from './transfer-api.service';

describe('TransferApiService', () => {
  let service: TransferApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransferApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(TransferApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar uma transferência', () => {
    const payload: CreateTransferRequest = {
      sourceAccountId: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
      destinationAccountId: '9ba25043-024c-4ba3-a48a-bf62a2c30ef0',
      amount: 250,
      date: '2026-09-02',
      description: 'Transferência para reserva',
    };
    const response: Transaction = {
      id: '2cb0ba91-bfc4-43be-89ec-336ca64a6231',
      description: payload.description,
      amount: payload.amount,
      competenceDate: payload.date,
      effectiveDate: payload.date,
      dueDate: null,
      type: 'TRANSFER',
      status: 'COMPLETED',
      paymentMethod: 'TRANSFER',
      sourceAccountId: payload.sourceAccountId,
      destinationAccountId: payload.destinationAccountId,
      categoryId: null,
      creditCardId: null,
      invoiceId: null,
      installmentGroupId: null,
      installmentNumber: null,
      installmentCount: null,
      createdAt: '2026-09-02T12:00:00Z',
      updatedAt: '2026-09-02T12:00:00Z',
    };

    service.create(payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne('/api/v1/transfers');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });
});
