import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { ToastService } from '../../../../core/feedback/toast/toast.service';
import { ApiRequestError } from '../../../../core/http/api-request-error';
import { CategoryApiService } from '../../../categories/data-access/category-api.service';
import { Category } from '../../../categories/models/category.models';
import { Transaction } from '../../../transactions/models/transaction.models';
import { CreditCardApiService } from '../../data-access/credit-card-api.service';
import { CreateCreditCardPurchaseRequest, CreditCard } from '../../models/credit-card.models';
import { CreditCardPurchaseCreatePage } from './credit-card-purchase-create-page';

describe('CreditCardPurchaseCreatePage', () => {
  let fixture: ComponentFixture<CreditCardPurchaseCreatePage>;
  let component: CreditCardPurchaseCreatePage;
  let categoryApi: { findAll: ReturnType<typeof vi.fn> };
  let creditCardApi: {
    findById: ReturnType<typeof vi.fn>;
    createPurchase: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const creditCard: CreditCard = {
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

  const expenseCategory: Category = {
    id: '1e207b3a-769a-42c6-bffc-2989bb091212',
    name: 'Mercado do Walter',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const incomeCategory: Category = {
    id: '2e207b3a-769a-42c6-bffc-2989bb091212',
    name: 'Salário do Saul',
    type: 'INCOME',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const purchaseRequest: CreateCreditCardPurchaseRequest = {
    description: 'Compra do Jesse',
    amount: 350.9,
    purchaseDate: '2026-09-16',
    categoryId: expenseCategory.id,
    installmentCount: 3,
  };

  const purchaseTransactions: Transaction[] = [
    {
      id: '3e207b3a-769a-42c6-bffc-2989bb091212',
      description: purchaseRequest.description,
      amount: 116.96,
      competenceDate: '2026-09-16',
      effectiveDate: null,
      dueDate: '2026-10-17',
      type: 'CREDIT_CARD_PURCHASE',
      status: 'COMPLETED',
      paymentMethod: 'CREDIT_CARD',
      sourceAccountId: null,
      destinationAccountId: null,
      categoryId: expenseCategory.id,
      creditCardId: creditCard.id,
      invoiceId: '4e207b3a-769a-42c6-bffc-2989bb091212',
      installmentGroupId: '5e207b3a-769a-42c6-bffc-2989bb091212',
      installmentNumber: 1,
      installmentCount: 3,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: '6e207b3a-769a-42c6-bffc-2989bb091212',
      description: purchaseRequest.description,
      amount: 116.97,
      competenceDate: '2026-10-16',
      effectiveDate: null,
      dueDate: '2026-11-17',
      type: 'CREDIT_CARD_PURCHASE',
      status: 'COMPLETED',
      paymentMethod: 'CREDIT_CARD',
      sourceAccountId: null,
      destinationAccountId: null,
      categoryId: expenseCategory.id,
      creditCardId: creditCard.id,
      invoiceId: '7e207b3a-769a-42c6-bffc-2989bb091212',
      installmentGroupId: '5e207b3a-769a-42c6-bffc-2989bb091212',
      installmentNumber: 2,
      installmentCount: 3,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
    {
      id: '8e207b3a-769a-42c6-bffc-2989bb091212',
      description: purchaseRequest.description,
      amount: 116.97,
      competenceDate: '2026-11-16',
      effectiveDate: null,
      dueDate: '2026-12-17',
      type: 'CREDIT_CARD_PURCHASE',
      status: 'COMPLETED',
      paymentMethod: 'CREDIT_CARD',
      sourceAccountId: null,
      destinationAccountId: null,
      categoryId: expenseCategory.id,
      creditCardId: creditCard.id,
      invoiceId: '9e207b3a-769a-42c6-bffc-2989bb091212',
      installmentGroupId: '5e207b3a-769a-42c6-bffc-2989bb091212',
      installmentNumber: 3,
      installmentCount: 3,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    categoryApi = {
      findAll: vi.fn().mockReturnValue(of([expenseCategory, incomeCategory])),
    };
    creditCardApi = {
      findById: vi.fn().mockReturnValue(of(creditCard)),
      createPurchase: vi.fn().mockReturnValue(of(purchaseTransactions)),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CreditCardPurchaseCreatePage],
      providers: [
        { provide: CategoryApiService, useValue: categoryApi },
        { provide: CreditCardApiService, useValue: creditCardApi },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: creditCard.id }),
            },
          },
        },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
  });

  function createPage(): void {
    fixture = TestBed.createComponent(CreditCardPurchaseCreatePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should load the active card and available categories', () => {
    createPage();

    expect(creditCardApi.findById).toHaveBeenCalledWith(creditCard.id);
    expect(categoryApi.findAll).toHaveBeenCalledOnce();
    expect(component.creditCard()).toEqual(creditCard);
    expect(component.categories()).toEqual([expenseCategory, incomeCategory]);
    expect(fixture.nativeElement.querySelector('app-credit-card-purchase-form')).not.toBeNull();
  });

  it('should render loading while card and categories are pending', () => {
    const cardSubject = new Subject<CreditCard>();
    const categoriesSubject = new Subject<Category[]>();
    creditCardApi.findById.mockReturnValue(cardSubject.asObservable());
    categoryApi.findAll.mockReturnValue(categoriesSubject.asObservable());

    createPage();

    expect(fixture.nativeElement.querySelector('app-skeleton')).not.toBeNull();

    cardSubject.next(creditCard);
    categoriesSubject.next([expenseCategory]);
    cardSubject.complete();
    categoriesSubject.complete();
  });

  it('should show an error and retry loading data', () => {
    creditCardApi.findById
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(creditCard));

    createPage();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível preparar a compra');

    (fixture.nativeElement.querySelector('.error-state button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
    expect(component.state()).toBe('success');
  });

  it('should block the purchase form for an inactive or blocked card', () => {
    creditCardApi.findById.mockReturnValue(of({ ...creditCard, status: 'BLOCKED' }));

    createPage();

    expect(fixture.nativeElement.querySelector('app-credit-card-purchase-form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Cartão indisponível para compras');
  });

  it('should create a purchase once and navigate to the refreshed card detail', () => {
    createPage();

    component.createPurchase(purchaseRequest);

    expect(creditCardApi.createPurchase).toHaveBeenCalledWith(creditCard.id, purchaseRequest);
    expect(toast.show).toHaveBeenCalledWith({
      tone: 'success',
      title: 'Compra registrada',
      message: '3 parcelas foram adicionadas às faturas correspondentes.',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/credit-cards', creditCard.id]);
  });

  it('should prevent double submission while the purchase request is pending', () => {
    const purchaseSubject = new Subject<Transaction[]>();
    creditCardApi.createPurchase.mockReturnValue(purchaseSubject.asObservable());

    createPage();
    component.createPurchase(purchaseRequest);
    component.createPurchase(purchaseRequest);

    expect(creditCardApi.createPurchase).toHaveBeenCalledOnce();
    expect(component.isSubmitting()).toBe(true);

    purchaseSubject.next(purchaseTransactions);
    purchaseSubject.complete();
  });

  it('should show a reload action after a conflict without retrying the post', () => {
    const conflict = new ApiRequestError({
      timestamp: '2026-09-16T10:00:00Z',
      status: 409,
      code: 'CREDIT_LIMIT_CONFLICT',
      message: 'Limite disponível insuficiente para realizar a compra.',
      path: `/api/v1/credit-cards/${creditCard.id}/purchases`,
      fieldErrors: [],
    });
    creditCardApi.createPurchase.mockReturnValue(throwError(() => conflict));

    createPage();
    component.createPurchase(purchaseRequest);
    fixture.detectChanges();

    expect(creditCardApi.createPurchase).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain(conflict.message);

    component.loadData();

    expect(creditCardApi.createPurchase).toHaveBeenCalledOnce();
    expect(creditCardApi.findById).toHaveBeenCalledTimes(2);
  });

  it('should navigate back to the credit card and categories when requested', () => {
    createPage();

    component.goBack();
    component.goToCategories();

    expect(router.navigate).toHaveBeenNthCalledWith(1, ['/credit-cards', creditCard.id]);
    expect(router.navigate).toHaveBeenNthCalledWith(2, ['/categories']);
  });
});
