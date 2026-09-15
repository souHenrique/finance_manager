import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/config/api-base-url';
import { Account, CreateAccountRequest, UpdateAccountRequest } from '../models/account.models';
import { AccountApiService } from './account-api.service';

describe('AccountApiService', () => {
  let service: AccountApiService;
  let httpMock: HttpTestingController;

  const account: Account = {
    id: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    name: 'Conta principal',
    type: 'CHECKING',
    institution: 'Banco Exemplo',
    initialBalance: 1500,
    currentBalance: 1320.5,
    status: 'ACTIVE',
    version: 0,
    createdAt: '2026-09-02T12:00:00Z',
    updatedAt: '2026-09-02T12:30:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: API_BASE_URL,
          useValue: '/api/v1',
        },
      ],
    });

    service = TestBed.inject(AccountApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve listar as contas', () => {
    service.findAll().subscribe((response) => {
      expect(response).toEqual([account]);
    });

    const request = httpMock.expectOne('/api/v1/accounts');

    expect(request.request.method).toBe('GET');

    request.flush([account]);
  });

  it('deve buscar uma conta pelo id', () => {
    service.findById(account.id).subscribe((response) => {
      expect(response).toEqual(account);
    });

    const request = httpMock.expectOne(`/api/v1/accounts/${account.id}`);

    expect(request.request.method).toBe('GET');

    request.flush(account);
  });

  it('deve criar uma conta', () => {
    const payload: CreateAccountRequest = {
      name: 'Conta principal',
      type: 'CHECKING',
      institution: 'Banco Exemplo',
      initialBalance: 1500,
    };

    service.create(payload).subscribe((response) => {
      expect(response).toEqual(account);
    });

    const request = httpMock.expectOne('/api/v1/accounts');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);

    request.flush(account);
  });

  it('deve atualizar uma conta', () => {
    const payload: UpdateAccountRequest = {
      name: 'Reserva de emergência',
      type: 'SAVINGS',
    };

    service.update(account.id, payload).subscribe((response) => {
      expect(response).toEqual(account);
    });

    const request = httpMock.expectOne(`/api/v1/accounts/${account.id}`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);

    request.flush(account);
  });

  it('deve atualizar o status da conta', () => {
    const updatedAccount: Account = {
      ...account,
      status: 'INACTIVE',
    };

    service
      .updateStatus(account.id, {
        status: 'INACTIVE',
      })
      .subscribe((response) => {
        expect(response).toEqual(updatedAccount);
      });

    const request = httpMock.expectOne(`/api/v1/accounts/${account.id}/status`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      status: 'INACTIVE',
    });

    request.flush(updatedAccount);
  });
});
