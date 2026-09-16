import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreditCard } from '../../../credit-cards/models/credit-card.models';
import { InvoiceFilterFormComponent } from './invoice-filter-form';

describe('InvoiceFilterFormComponent', () => {
  let fixture: ComponentFixture<InvoiceFilterFormComponent>;
  let component: InvoiceFilterFormComponent;

  const activeCard: CreditCard = {
    id: '2c6c0297-275b-4b06-8078-05f00c1e3f4f',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: 'db2f76f2-b23b-4aa0-99b2-51864215b6fa',
    status: 'ACTIVE',
    version: 2,
  };

  const inactiveCard: CreditCard = {
    ...activeCard,
    id: '75b61f90-23aa-4f3a-a203-0b78b4e3f0a0',
    name: 'Cartão Saul',
    status: 'INACTIVE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvoiceFilterFormComponent],
    }).compileComponents();
  });

  function createComponent(submitting = false): void {
    fixture = TestBed.createComponent(InvoiceFilterFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('creditCards', [activeCard, inactiveCard]);
    fixture.componentRef.setInput('submitting', submitting);
    fixture.detectChanges();
  }

  it('should render all cards, including inactive cards for historical searches', () => {
    createComponent();

    const options = Array.from(
      fixture.nativeElement.querySelectorAll(
        '#invoice-credit-card option',
      ) as NodeListOf<HTMLOptionElement>,
    ).map((option) => option.textContent?.trim());

    expect(options).toContain('Cartão Heisenberg');
    expect(
      options.some((option) => option?.includes('Cartão Saul') && option.includes('Inativo')),
    ).toBe(true);
  });

  it('should emit combined filters without empty values', () => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.form.setValue({
      creditCardId: activeCard.id,
      referenceMonth: 9,
      referenceYear: 2026,
      status: 'CLOSED',
    });

    component.apply();

    expect(applied).toHaveBeenCalledWith({
      creditCardId: activeCard.id,
      referenceMonth: 9,
      referenceYear: 2026,
      status: 'CLOSED',
    });
  });

  it('should omit empty values from filters', () => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.apply();

    expect(applied).toHaveBeenCalledWith({});
  });

  it.each([
    ['referenceMonth', 13],
    ['referenceMonth', 1.5],
    ['referenceYear', 0],
    ['referenceYear', 2026.5],
  ] as const)('should not emit invalid %s: %s', (controlName, value) => {
    createComponent();
    const applied = vi.fn();
    component.applied.subscribe(applied);

    component.form.controls[controlName].setValue(value);
    component.apply();

    expect(applied).not.toHaveBeenCalled();
    expect(component.form.controls[controlName].touched).toBe(true);
  });

  it('should reset fields and emit cleared', () => {
    createComponent();
    const cleared = vi.fn();
    component.cleared.subscribe(cleared);

    component.form.setValue({
      creditCardId: activeCard.id,
      referenceMonth: 9,
      referenceYear: 2026,
      status: 'OPEN',
    });

    component.clear();

    expect(component.form.getRawValue()).toEqual({
      creditCardId: '',
      referenceMonth: null,
      referenceYear: null,
      status: null,
    });
    expect(cleared).toHaveBeenCalledOnce();
  });

  it('should synchronize fields when the parent changes initial filters', () => {
    createComponent();

    fixture.componentRef.setInput('initialFilters', {
      creditCardId: inactiveCard.id,
      referenceMonth: 12,
      referenceYear: 2025,
      status: 'PAID',
    });
    fixture.detectChanges();

    expect(component.form.getRawValue()).toEqual({
      creditCardId: inactiveCard.id,
      referenceMonth: 12,
      referenceYear: 2025,
      status: 'PAID',
    });
  });

  it('should disable actions while the invoice list is loading', () => {
    createComponent(true);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons).toHaveLength(2);
    expect([...buttons].every((button) => button.disabled)).toBe(true);
  });
});
