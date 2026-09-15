import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Account } from '../../models/account.models';
import { AccountFormComponent, AccountFormMode } from './account-form';

describe('AccountFormComponent', () => {
  let fixture: ComponentFixture<AccountFormComponent>;
  let component: AccountFormComponent;

  const account: Account = {
    id: 'fdb57d8f-7c0c-4abc-9e6a-7e6d0f9a7db8',
    name: 'Conta principal',
    type: 'DIGITAL_ACCOUNT',
    institution: 'Banco Aurora',
    initialBalance: 1500,
    currentBalance: 1820.5,
    status: 'ACTIVE',
    version: 3,
    createdAt: '2026-09-01T10:00:00Z',
    updatedAt: '2026-09-15T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountFormComponent],
    }).compileComponents();
  });

  function createComponent(
    mode: AccountFormMode = 'create',
    currentAccount: Account | null = null,
    submitting = false,
  ): void {
    fixture = TestBed.createComponent(AccountFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', mode);
    fixture.componentRef.setInput('account', currentAccount);
    fixture.componentRef.setInput('submitting', submitting);
    fixture.detectChanges();
  }

  function getInput(id: string): HTMLInputElement {
    return fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
  }

  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;

    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it('should render all fields, including initial balance, in create mode', () => {
    createComponent();

    expect(getInput('name')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#type')).not.toBeNull();
    expect(getInput('institution')).not.toBeNull();
    expect(getInput('initialBalance')).not.toBeNull();
  });

  it('should emit a valid create request with the initial balance', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.setValue({
      name: '  Reserva mensal  ',
      type: 'SAVINGS',
      institution: '  Banco Horizonte  ',
      initialBalance: 2500.75,
    });
    submitForm();

    expect(submitted).toHaveBeenCalledWith({
      create: {
        name: 'Reserva mensal',
        type: 'SAVINGS',
        institution: 'Banco Horizonte',
        initialBalance: 2500.75,
      },
      update: {
        name: 'Reserva mensal',
        type: 'SAVINGS',
        institution: 'Banco Horizonte',
      },
    });
  });

  it('should accept a negative initial balance', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.setValue({
      name: 'Conta em débito',
      type: 'CHECKING',
      institution: '',
      initialBalance: -320.4,
    });
    submitForm();

    expect(submitted).toHaveBeenCalledWith(
      expect.objectContaining({
        create: expect.objectContaining({
          initialBalance: -320.4,
          institution: null,
        }),
      }),
    );
  });

  it('should not render the initial balance field in edit mode', () => {
    createComponent('edit', account);

    expect(fixture.nativeElement.querySelector('#initialBalance')).toBeNull();
  });

  it('should populate the editable fields from the received account', () => {
    createComponent('edit', account);

    expect(getInput('name').value).toBe(account.name);
    expect((fixture.nativeElement.querySelector('#type') as HTMLSelectElement).value).toBe(
      account.type,
    );
    expect(getInput('institution').value).toBe(account.institution);
  });

  it('should not emit a submit event when required fields are invalid', () => {
    createComponent();
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.controls.name.setValue('');
    submitForm();

    expect(submitted).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBe(true);
  });

  it('should emit cancelled when the user clicks cancel', () => {
    createComponent();
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);

    const cancelButton = fixture.nativeElement.querySelectorAll(
      'app-button button',
    )[0] as HTMLButtonElement;

    cancelButton.click();

    expect(cancelled).toHaveBeenCalledOnce();
  });

  it('should disable both actions while submitting', () => {
    createComponent('create', null, true);

    const buttons = fixture.nativeElement.querySelectorAll(
      'app-button button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons).toHaveLength(2);
    expect([...buttons].every((button) => button.disabled)).toBe(true);
  });
});
