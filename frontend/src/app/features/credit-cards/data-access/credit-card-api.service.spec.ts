import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Transaction } from '../../transactions/models/transaction.models';
import {
  CreateCreditCardPurchaseRequest,
  CreateCreditCardRequest,
  CreditCard,
  CreditCardRefund,
  CreditCardRefundRequest,
  UpdateCreditCardRequest,
} from '../models/credit-card.models';
import { CreditCardApiService } from './credit-card-api.service';

describe('CreditCardApiService', () => {
  let service: CreditCardApiService;
  let httpMock: HttpTestingController;

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão principal',
    creditLimit: 5000,
    availableLimit: 4200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    status: 'ACTIVE',
    version: 0,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CreditCardApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(CreditCardApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar um cartão', () => {
    const payload: CreateCreditCardRequest = {
      name: creditCard.name,
      creditLimit: creditCard.creditLimit,
      closingDay: creditCard.closingDay,
      dueDay: creditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    };

    service.create(payload).subscribe((response) => {
      expect(response).toEqual(creditCard);
    });

    const request = httpMock.expectOne('/api/v1/credit-cards');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(creditCard);
  });

  it('deve listar os cartões', () => {
    service.findAll().subscribe((response) => {
      expect(response).toEqual([creditCard]);
    });

    const request = httpMock.expectOne('/api/v1/credit-cards');

    expect(request.request.method).toBe('GET');

    request.flush([creditCard]);
  });

  it('deve buscar um cartão pelo id', () => {
    service.findById(creditCard.id).subscribe((response) => {
      expect(response).toEqual(creditCard);
    });

    const request = httpMock.expectOne(`/api/v1/credit-cards/${creditCard.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(creditCard);
  });

  it('deve atualizar um cartão', () => {
    const payload: UpdateCreditCardRequest = {
      name: 'Cartão viagens',
      creditLimit: 6500,
      status: 'BLOCKED',
    };
    const response: CreditCard = {
      ...creditCard,
      ...payload,
      version: 1,
    };

    service.update(creditCard.id, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(`/api/v1/credit-cards/${creditCard.id}`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });

  it('deve criar uma compra no cartão', () => {
    const payload: CreateCreditCardPurchaseRequest = {
      description: 'Compra no supermercado',
      amount: 350.9,
      purchaseDate: '2026-09-04',
      categoryId: '57b1879c-a98e-4718-b66d-47f970ab6709',
      installmentCount: 1,
    };
    const transaction: Transaction = {
      id: '2cb0ba91-bfc4-43be-89ec-336ca64a6231',
      description: payload.description,
      amount: payload.amount,
      competenceDate: payload.purchaseDate,
      effectiveDate: null,
      dueDate: null,
      type: 'CREDIT_CARD_PURCHASE',
      status: 'COMPLETED',
      paymentMethod: 'CREDIT_CARD',
      sourceAccountId: null,
      destinationAccountId: null,
      categoryId: payload.categoryId,
      creditCardId: creditCard.id,
      invoiceId: '72486234-ef50-4c7e-99a7-9193a28533a8',
      installmentGroupId: null,
      installmentNumber: 1,
      installmentCount: 1,
      createdAt: '2026-09-04T12:00:00Z',
      updatedAt: '2026-09-04T12:00:00Z',
    };

    service.createPurchase(creditCard.id, payload).subscribe((response) => {
      expect(response).toEqual([transaction]);
    });

    const request = httpMock.expectOne(`/api/v1/credit-cards/${creditCard.id}/purchases`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush([transaction]);
  });

  it('deve estornar uma compra no cartão', () => {
    const transactionId = '2cb0ba91-bfc4-43be-89ec-336ca64a6231';
    const payload: CreditCardRefundRequest = {
      reason: 'Compra cancelada pelo estabelecimento',
    };
    const response: CreditCardRefund = {
      id: '56df9f23-ad47-4d69-a241-4824d870e79b',
      creditCardId: creditCard.id,
      selectedTransactionId: transactionId,
      installmentGroupId: null,
      reason: payload.reason,
      totalAmount: 350.9,
      limitRestoredAmount: 350.9,
      paidCompensationAmount: 0,
      createdAt: '2026-09-05T12:00:00Z',
      items: [
        {
          id: '5296024f-8f22-4cae-b132-b5c817d7e922',
          originalTransactionId: transactionId,
          originalInvoiceId: '72486234-ef50-4c7e-99a7-9193a28533a8',
          originalInvoiceStatus: 'OPEN',
          amount: 350.9,
          treatment: 'UNPAID_CANCELLATION',
          creditId: null,
        },
      ],
    };

    service.refundPurchase(creditCard.id, transactionId, payload).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpMock.expectOne(
      `/api/v1/credit-cards/${creditCard.id}/purchase/${transactionId}/refund`,
    );

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(response);
  });
});
