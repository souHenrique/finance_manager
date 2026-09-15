import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Account } from '../../../accounts/models/account.models';
import { Category } from '../../../categories/models/category.models';
import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { TransactionFilterFormComponent } from './transaction-filter-form';

describe('TransactionFilterFormComponent', () => {
  let fixture: ComponentFixture<TransactionFilterFormComponent>;
  let component: TransactionFilterFormComponent;

  const account: Account = {
    id: 'b37670b2-e846-4b4a-a61c-29e635b64ceb',
    name: 'Conta Walter',
    type: 'CHECKING',
    institution: 'Banco Albuquerque',
    initialBalance: 1200,
    currentBalance: 1300,
    status: 'ACTIVE',
    version: 1,
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const category: Category = {
    id: 'd1ed3d5c-3cb5-4c88-a5b8-3c506e42be29',
    name: 'Jesse Pinkman',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-15T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  const creditCard: CreditCard = {
    id: '0b8aa90e-687a-48d1-b104-0a9ed1872ed2',
    name: 'Cartão Saul',
    creditLimit: 5000,
    availableLimit: 4300,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: account.id,
    status: 'ACTIVE',
    version: 1,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionFilterFormComponent],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(TransactionFilterFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('accounts', [account]);
    fixture.componentRef.setInput('categories', [category]);
    fixture.componentRef.setInput('creditCards', [creditCard]);
    fixture.detectChanges();
  }

  it('should emit combined filters without empty values', () => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.form.setValue({
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      categoryId: category.id,
      accountId: account.id,
      creditCardId: creditCard.id,
      type: 'EXPENSE',
      status: 'COMPLETED',
      minAmount: 50.25,
      maxAmount: 500.75,
      description: '  mercado  ',
    });

    component.apply();

    expect(applied).toHaveBeenCalledWith({
      startDate: '2026-09-01',
      endDate: '2026-09-30',
      categoryId: category.id,
      accountId: account.id,
      creditCardId: creditCard.id,
      type: 'EXPENSE',
      status: 'COMPLETED',
      minAmount: 50.25,
      maxAmount: 500.75,
      description: 'mercado',
    });
  });

  it('should not emit invalid date or amount ranges', () => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.form.patchValue({
      startDate: '2026-09-30',
      endDate: '2026-09-01',
      minAmount: 500,
      maxAmount: 50,
    });
    component.apply();

    expect(applied).not.toHaveBeenCalled();
    expect(component.form.errors).toEqual(
      expect.objectContaining({
        invalidDateRange: true,
        invalidAmountRange: true,
      }),
    );
  });

  it('should omit whitespace descriptions and NaN amounts', () => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.form.patchValue({
      minAmount: Number.NaN,
      description: '   ',
    });
    component.apply();

    expect(applied).toHaveBeenCalledWith({});
  });

  it('should reset fields and emit cleared', () => {
    createComponent();
    const cleared = vi.fn();
    component.cleared.subscribe(cleared);
    component.form.patchValue({
      description: 'Jesse Pinkman',
      minAmount: 75,
    });

    component.clear();

    expect(component.form.getRawValue()).toEqual({
      startDate: '',
      endDate: '',
      categoryId: '',
      accountId: '',
      creditCardId: '',
      type: null,
      status: null,
      minAmount: null,
      maxAmount: null,
      description: '',
    });
    expect(cleared).toHaveBeenCalledOnce();
  });
});
