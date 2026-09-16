import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreditCard } from '../../models/credit-card.models';
import { CreditCardListPage } from './credit-card-list-page';

describe('CreditCardListPage', () => {
  let fixture: ComponentFixture<CreditCardListPage>;
  let creditCardApi: { findAll: ReturnType<typeof vi.fn> };
  let accountApi: { findAll: ReturnType<typeof vi.fn> };

  const account: Account = {
    id: '0f6d7313-77f8-4b48-a63d-5338dd95461e',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1500,
    currentBalance: 1800,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const creditCards: CreditCard[] = [
    {
      id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
      name: 'Cartão Heisenberg',
      creditLimit: 5000,
      availableLimit: 3200,
      closingDay: 10,
      dueDay: 17,
      defaultAccountId: account.id,
      status: 'ACTIVE',
      version: 2,
    },
    {
      id: '1a4cdb29-3794-46f8-b0ea-694431aa6611',
      name: 'Cartão Saul',
      creditLimit: 3000,
      availableLimit: 900,
      closingDay: 5,
      dueDay: 12,
      defaultAccountId: account.id,
      status: 'BLOCKED',
      version: 1,
    },
  ];

  beforeEach(async () => {
    creditCardApi = { findAll: vi.fn().mockReturnValue(of(creditCards)) };
    accountApi = { findAll: vi.fn().mockReturnValue(of([account])) };

    await TestBed.configureTestingModule({
      imports: [CreditCardListPage],
      providers: [
        provideRouter([]),
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: AccountApiService, useValue: accountApi },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(CreditCardListPage);
    fixture.detectChanges();
  }

  it('should show skeletons while cards and accounts are loading', () => {
    const creditCardResponse = new Subject<CreditCard[]>();
    const accountResponse = new Subject<Account[]>();
    creditCardApi.findAll.mockReturnValue(creditCardResponse.asObservable());
    accountApi.findAll.mockReturnValue(accountResponse.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelectorAll('app-skeleton')).toHaveLength(3);

    creditCardResponse.next(creditCards);
    creditCardResponse.complete();
    accountResponse.next([account]);
    accountResponse.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-skeleton')).toBeNull();
  });

  it('should render cards, limits, statuses, and default account names', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(creditCardApi.findAll).toHaveBeenCalledOnce();
    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(content).toContain('Cartão Heisenberg');
    expect(content).toContain('Cartão Saul');
    expect(content).toContain('Conta Walter');
    expect(content).toContain('Limite total');
    expect(content).toContain('Limite disponível');
    expect(content).toContain('Ativo');
    expect(content).toContain('Bloqueado');
    expect(content).toContain('Dia 10');
    expect(content).toContain('Dia 17');
  });

  it('should display an error state and retry the loading', () => {
    creditCardApi.findAll
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(creditCards));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();

    const retryButton = fixture.nativeElement.querySelector(
      '.error-state button',
    ) as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(creditCardApi.findAll).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Cartão Heisenberg');
  });

  it('should show an empty state when no card has been created', () => {
    creditCardApi.findAll.mockReturnValue(of([]));

    createPage();

    expect(fixture.nativeElement.querySelector('app-empty-state')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nenhum cartão cadastrado');
    expect(fixture.nativeElement.textContent).toContain('Novo cartão');
  });

  it('should expose readable labels for every card status', () => {
    createPage();

    expect(fixture.componentInstance.statusLabel('ACTIVE')).toBe('Ativo');
    expect(fixture.componentInstance.statusLabel('INACTIVE')).toBe('Inativo');
    expect(fixture.componentInstance.statusLabel('BLOCKED')).toBe('Bloqueado');
  });
});
