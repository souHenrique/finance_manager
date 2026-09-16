import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AccountApiService } from '../../../accounts/data-access/account-api.service';
import { Account } from '../../../accounts/models/account.models';
import { AppDialogService } from '../../../../core/feedback/dialog/dialog.service';
import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreditCard } from '../../models/credit-card.models';
import { CreditCardDetailPage } from './credit-card-detail-page';

describe('CreditCardDetailPage', () => {
  let fixture: ComponentFixture<CreditCardDetailPage>;
  let component: CreditCardDetailPage;
  let accountApi: { findAll: ReturnType<typeof vi.fn> };
  let creditCardApi: {
    findById: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let dialog: { confirm: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

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

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: account.id,
    status: 'ACTIVE',
    version: 2,
  };

  beforeEach(async () => {
    accountApi = { findAll: vi.fn().mockReturnValue(of([account])) };
    creditCardApi = {
      findById: vi.fn().mockReturnValue(of(creditCard)),
      update: vi.fn().mockReturnValue(of({ ...creditCard, status: 'BLOCKED' })),
    };
    dialog = { confirm: vi.fn().mockReturnValue(of(true)) };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreditCardDetailPage],
      providers: [
        { provide: AccountApiService, useValue: accountApi },
        { provide: CreditCardApiService, useValue: creditCardApi },
        { provide: AppDialogService, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: creditCard.id }),
            },
          },
        },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(CreditCardDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load and display card data from the route id', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(creditCardApi.findById).toHaveBeenCalledWith(creditCard.id);
    expect(accountApi.findAll).toHaveBeenCalledOnce();
    expect(content).toContain('Cartão Heisenberg');
    expect(content).toContain('Conta Walter');
    expect(content).toContain('Limite total');
    expect(content).toContain('Limite disponível');
    expect(content).toContain('Limite comprometido');
    expect(content).toContain('Ativo');
  });

  it('should show an error state when loading fails', () => {
    creditCardApi.findById.mockReturnValue(throwError(() => new Error('network')));

    createPage();

    expect(fixture.nativeElement.querySelector('app-error-state')).not.toBeNull();
  });

  it('should expose edit and active-card status actions', () => {
    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(content).toContain('Editar');
    expect(content).toContain('Bloquear cartão');
    expect(content).toContain('Inativar cartão');
  });

  it('should not update card status when confirmation is cancelled', () => {
    dialog.confirm.mockReturnValue(of(false));

    createPage();
    component.confirmStatusChange('BLOCKED');

    expect(dialog.confirm).toHaveBeenCalledOnce();
    expect(creditCardApi.update).not.toHaveBeenCalled();
    expect(toast.show).not.toHaveBeenCalled();
  });

  it('should block an active card, show feedback, and reload its data', () => {
    createPage();
    component.confirmStatusChange('BLOCKED');

    expect(creditCardApi.update).toHaveBeenCalledWith(creditCard.id, {
      status: 'BLOCKED',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Cartão bloqueado',
      message: 'O cartão não poderá receber novas compras enquanto estiver bloqueado.',
    });
    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
    expect(accountApi.findAll).toHaveBeenCalledTimes(2);
  });

  it('should activate a blocked card', () => {
    const blockedCreditCard: CreditCard = { ...creditCard, status: 'BLOCKED' };
    creditCardApi.findById.mockReturnValue(of(blockedCreditCard));
    creditCardApi.update.mockReturnValue(of({ ...blockedCreditCard, status: 'ACTIVE' }));

    createPage();
    component.confirmStatusChange('ACTIVE');

    expect(creditCardApi.update).toHaveBeenCalledWith(creditCard.id, {
      status: 'ACTIVE',
    });
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Cartão ativado',
      message: 'O cartão está disponível novamente para novas compras.',
    });
  });

  it('should keep only activation available for an inactive card', () => {
    creditCardApi.findById.mockReturnValue(of({ ...creditCard, status: 'INACTIVE' }));

    createPage();

    const content = fixture.nativeElement.textContent as string;

    expect(content).toContain('Ativar cartão');
    expect(content).not.toContain('Bloquear cartão');
    expect(content).not.toContain('Inativar cartão');
  });

  it('should navigate to card editing and back to the card list', () => {
    createPage();

    component.goToEdit();
    component.goBack();

    expect(router.navigate).toHaveBeenNthCalledWith(1, ['/credit-cards', creditCard.id, 'edit']);
    expect(router.navigate).toHaveBeenNthCalledWith(2, ['/credit-cards']);
  });
});
