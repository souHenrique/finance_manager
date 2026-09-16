import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Account } from '../../../accounts/models/account.models';
import { CreditCard } from '../../models/credit-card.models';
import { CreditCardFormComponent, CreditCardFormMode } from './credit-card-form';

describe('CreditCardFormComponent', () => {
  let fixture: ComponentFixture<CreditCardFormComponent>;
  let component: CreditCardFormComponent;

  const accounts: Account[] = [
    {
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
    },
    {
      id: '49c3a30a-7df5-4602-b25e-11fcd2e9d4e0',
      name: 'Conta Jesse',
      type: 'SAVINGS',
      institution: 'Banco Pinkman',
      initialBalance: 500,
      currentBalance: 700,
      status: 'INACTIVE',
      version: 1,
      createdAt: '2026-09-16T10:00:00Z',
      updatedAt: '2026-09-16T10:00:00Z',
    },
  ];

  const creditCard: CreditCard = {
    id: 'd89835ee-3463-4a35-a2e9-38d96ab17418',
    name: 'Cartão Heisenberg',
    creditLimit: 5000,
    availableLimit: 3200,
    closingDay: 10,
    dueDay: 17,
    defaultAccountId: accounts[0].id,
    status: 'ACTIVE',
    version: 2,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreditCardFormComponent],
    }).compileComponents();
  });

  function createComponent(
    mode: CreditCardFormMode = 'create',
    currentCreditCard: CreditCard | null = null,
    currentAccounts: Account[] = accounts,
    submitting = false,
  ): void {
    fixture = TestBed.createComponent(CreditCardFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('creditCard', currentCreditCard);
    fixture.componentRef.setInput('accounts', currentAccounts);
    fixture.componentRef.setInput('submitting', submitting);
    fixture.detectChanges();
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it('should render the fields required to create a credit card', () => {
    createComponent();

    expect(fixture.nativeElement.querySelector('#name')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#credit-limit')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#closing-day')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#due-day')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#default-account-id')).not.toBeNull();
  });

  it('should emit a valid create request without an available limit', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.setValue({
      name: '  Cartão Saul  ',
      creditLimit: 4500.5,
      closingDay: 8,
      dueDay: 15,
      defaultAccountId: accounts[0].id,
    });
    submitForm();

    expect(submitted).toHaveBeenCalledWith({
      create: {
        name: 'Cartão Saul',
        creditLimit: 4500.5,
        closingDay: 8,
        dueDay: 15,
        defaultAccountId: accounts[0].id,
      },
      update: {
        name: 'Cartão Saul',
        creditLimit: 4500.5,
        closingDay: 8,
        dueDay: 15,
        defaultAccountId: accounts[0].id,
      },
    });
  });

  it('should not show inactive accounts as payment options', () => {
    createComponent();

    const options = Array.from(
      fixture.nativeElement.querySelectorAll(
        '#default-account-id option',
      ) as NodeListOf<HTMLOptionElement>,
    ).map((option) => option.textContent?.trim());

    expect(options).toContain('Conta Walter — Banco Albuquerque');
    expect(options).not.toContain('Conta Jesse — Banco Pinkman');
  });

  it('should populate editable fields in edit mode', () => {
    createComponent('edit', creditCard);

    expect(component.form.getRawValue()).toEqual({
      name: creditCard.name,
      creditLimit: creditCard.creditLimit,
      closingDay: creditCard.closingDay,
      dueDay: creditCard.dueDay,
      defaultAccountId: creditCard.defaultAccountId,
    });
    expect(fixture.nativeElement.textContent).not.toContain('Limite disponível');
    expect(fixture.nativeElement.textContent).not.toContain('Status');
  });

  it('should prevent a limit lower than the committed amount in edit mode', () => {
    createComponent('edit', creditCard);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.controls.creditLimit.setValue(1700);
    submitForm();

    expect(component.form.controls.creditLimit.hasError('min')).toBe(true);
    expect(submitted).not.toHaveBeenCalled();
  });

  it('should not emit when a required field is invalid', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.controls.name.setValue('');
    submitForm();

    expect(submitted).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBe(true);
  });

  it('should not emit when no active account is available', () => {
    createComponent('create', null, [accounts[1]]);
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.setValue({
      name: 'Cartão Gus',
      creditLimit: 3000,
      closingDay: 5,
      dueDay: 12,
      defaultAccountId: accounts[1].id,
    });
    submitForm();

    expect(submitted).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'É necessário ter pelo menos uma conta ativa',
    );
  });

  it('should emit cancelled when the cancel button is pressed', () => {
    createComponent();
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);

    const cancelButton = fixture.nativeElement.querySelectorAll(
      'app-button button',
    )[0] as HTMLButtonElement;
    cancelButton.click();

    expect(cancelled).toHaveBeenCalledOnce();
  });

  it('should disable actions while submitting', () => {
    createComponent('create', null, accounts, true);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons).toHaveLength(2);
    expect([...buttons].every((button) => button.disabled)).toBe(true);
  });
});
