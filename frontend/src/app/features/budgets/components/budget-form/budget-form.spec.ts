import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Category } from '../../../categories/models/category.models';
import { Budget } from '../../models/budget.models';
import { BudgetFormComponent } from './budget-form';

describe('BudgetFormComponent', () => {
  let fixture: ComponentFixture<BudgetFormComponent>;
  let component: BudgetFormComponent;

  const category: Category = {
    id: '57b1879c-a98e-4718-b66d-47f970ab6709',
    name: 'Alimentação',
    type: 'EXPENSE',
    parentCategoryId: null,
    status: 'ACTIVE',
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  const budget: Budget = {
    id: 'c487c4cf-d948-4ba8-a85f-e36bb798c928',
    categoryId: category.id,
    month: 9,
    year: 2026,
    amountLimit: 1500,
    spentAmount: 1200,
    usagePercentage: 80,
    alertStatus: 'ALERT',
    createdAt: '2026-09-16T10:00:00Z',
    updatedAt: '2026-09-16T10:00:00Z',
  };

  function createForm(mode: 'create' | 'edit', selectedBudget: Budget | null = null): void {
    fixture = TestBed.createComponent(BudgetFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('budget', selectedBudget);
    fixture.componentRef.setInput('categories', [category]);
    fixture.componentRef.setInput('defaultMonth', 9);
    fixture.componentRef.setInput('defaultYear', 2026);
    fixture.componentRef.setInput('submitting', false);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BudgetFormComponent],
    }).compileComponents();
  });

  it('should emit a valid request when creating a budget', () => {
    createForm('create');
    const created = vi.fn();
    component.created.subscribe(created);

    component.form.setValue({
      categoryId: category.id,
      month: 10,
      year: 2026,
      amountLimit: 1750.5,
    });
    component.submit();

    expect(created).toHaveBeenCalledWith({
      categoryId: category.id,
      month: 10,
      year: 2026,
      amountLimit: 1750.5,
    });
  });

  it('should load a budget and emit only changed fields in edit mode', () => {
    createForm('edit', budget);
    const updated = vi.fn();
    component.updated.subscribe(updated);

    expect(component.form.getRawValue()).toEqual({
      categoryId: budget.categoryId,
      month: budget.month,
      year: budget.year,
      amountLimit: budget.amountLimit,
    });

    component.form.controls.amountLimit.setValue(1800);
    component.submit();

    expect(updated).toHaveBeenCalledWith({ amountLimit: 1800 });
  });

  it('should not submit invalid values', () => {
    createForm('create');
    const created = vi.fn();
    component.created.subscribe(created);

    component.submit();

    expect(created).not.toHaveBeenCalled();
    expect(component.form.controls.categoryId.touched).toBe(true);
  });

  it('should not submit unchanged values in edit mode', () => {
    createForm('edit', budget);
    const updated = vi.fn();
    component.updated.subscribe(updated);
    component.submit();

    expect(updated).not.toHaveBeenCalled();
  });

  it('should reject a limit with more than two decimal places', () => {
    createForm('create');
    const created = vi.fn();
    component.created.subscribe(created);

    component.form.setValue({
      categoryId: category.id,
      month: 9,
      year: 2026,
      amountLimit: 100.999,
    });
    component.submit();

    expect(created).not.toHaveBeenCalled();
    expect(component.form.controls.amountLimit.hasError('decimalPlaces')).toBe(true);
  });

  it('should disable actions while the form is submitting and emit cancellation', () => {
    createForm('create');
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);
    fixture.componentRef.setInput('submitting', true);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons[0].disabled).toBe(true);
    expect(buttons[1].disabled).toBe(true);

    fixture.componentRef.setInput('submitting', false);
    fixture.detectChanges();
    buttons[0].click();

    expect(cancelled).toHaveBeenCalledOnce();
  });
});
