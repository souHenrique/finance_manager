import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Category } from '../../../categories/models/category.models';
import { CreditCardPurchaseFormComponent } from './credit-card-purchase-form';

describe('CreditCardPurchaseFormComponent', () => {
  let fixture: ComponentFixture<CreditCardPurchaseFormComponent>;
  let component: CreditCardPurchaseFormComponent;

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

  const inactiveExpenseCategory: Category = {
    id: '3e207b3a-769a-42c6-bffc-2989bb091212',
    name: 'Restaurante do Gus',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'INACTIVE',
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreditCardPurchaseFormComponent],
    }).compileComponents();
  });

  function createComponent(
    categories: Category[] = [expenseCategory, incomeCategory, inactiveExpenseCategory],
    submitting = false,
  ): void {
    fixture = TestBed.createComponent(CreditCardPurchaseFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('categories', categories);
    fixture.componentRef.setInput('submitting', submitting);
    fixture.detectChanges();
  }

  function fillValidForm(installmentCount = 1): void {
    component.form.setValue({
      description: '  Compra do Jesse  ',
      amount: 350.9,
      purchaseDate: '2026-09-16',
      categoryId: expenseCategory.id,
      installmentCount,
    });
  }

  it('should show only active expense categories', () => {
    createComponent();

    const options = Array.from(
      fixture.nativeElement.querySelectorAll(
        '#purchase-category option',
      ) as NodeListOf<HTMLOptionElement>,
    ).map((option) => option.textContent?.trim());

    expect(options).toContain(expenseCategory.name);
    expect(options).not.toContain(incomeCategory.name);
    expect(options).not.toContain(inactiveExpenseCategory.name);
  });

  it('should emit an upfront purchase with installmentCount one', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    fillValidForm();
    component.submit();

    expect(submitted).toHaveBeenCalledWith({
      description: 'Compra do Jesse',
      amount: 350.9,
      purchaseDate: '2026-09-16',
      categoryId: expenseCategory.id,
      installmentCount: 1,
    });
  });

  it('should emit the selected installment count without calculating installment amounts', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    fillValidForm(3);
    component.submit();

    expect(submitted).toHaveBeenCalledWith(
      expect.objectContaining({
        amount: 350.9,
        installmentCount: 3,
      }),
    );
  });

  it.each([0, -30, 1.5])('should not emit an invalid amount or installment count: %s', (value) => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidForm();

    if (value === 1.5) {
      component.form.controls.installmentCount.setValue(value);
    } else {
      component.form.controls.amount.setValue(value);
    }

    component.submit();

    expect(submitted).not.toHaveBeenCalled();
  });

  it('should not emit when no active expense category exists', () => {
    createComponent([incomeCategory, inactiveExpenseCategory]);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.setValue({
      description: 'Compra impossível do Hector',
      amount: 100,
      purchaseDate: '2026-09-16',
      categoryId: incomeCategory.id,
      installmentCount: 1,
    });
    component.submit();

    expect(submitted).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Cadastre ou ative uma categoria de despesa',
    );
  });

  it('should disable actions and prevent submission while submitting', () => {
    createComponent(undefined, true);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);
    fillValidForm();
    component.submit();

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(submitted).not.toHaveBeenCalled();
    expect([...buttons].every((button) => button.disabled)).toBe(true);
  });

  it('should emit cancelled when the user presses cancel', () => {
    createComponent();
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);

    const cancelButton = fixture.nativeElement.querySelectorAll(
      'app-button button',
    )[0] as HTMLButtonElement;
    cancelButton.click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
