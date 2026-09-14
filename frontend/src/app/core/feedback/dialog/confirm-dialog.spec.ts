import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Button } from '../../../shared/ui/button/button';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from './confirm-dialog';

describe('ConfirmDialog', () => {
  let fixture: ComponentFixture<ConfirmDialog>;
  let close: ReturnType<typeof vi.fn>;

  const dialogData: ConfirmDialogData = {
    id: 'confirm-dialog-test',
    title: 'Excluir orçamento',
    message: 'Essa ação não poderá ser desfeita.',
    confirmLabel: 'Excluir',
    cancelLabel: 'Cancelar',
    danger: true,
  };

  beforeEach(async () => {
    close = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ConfirmDialog],
      providers: [
        {
          provide: DIALOG_DATA,
          useValue: dialogData,
        },
        {
          provide: DialogRef,
          useValue: {
            close,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialog);
    fixture.detectChanges();
  });

  it('should render title and description with accessible identifiers', () => {
    const element = fixture.nativeElement as HTMLElement;
    const title = element.querySelector('h2');
    const description = element.querySelector('p');

    expect(title?.id).toBe('confirm-dialog-test-title');
    expect(title?.textContent).toContain('Excluir orçamento');

    expect(description?.id).toBe('confirm-dialog-test-description');
    expect(description?.textContent).toContain(
      'Essa ação não poderá ser desfeita.',
    );
  });

  it('should render the configured button labels', () => {
    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(buttons).toHaveLength(2);
    expect(buttons[0]?.textContent).toContain('Cancelar');
    expect(buttons[1]?.textContent).toContain('Excluir');
  });

  it('should close with false when cancel is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;

    buttons[0]?.click();

    expect(close).toHaveBeenCalledOnce();
    expect(close).toHaveBeenCalledWith(false);
  });

  it('should close with true when confirm is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;

    buttons[1]?.click();

    expect(close).toHaveBeenCalledOnce();
    expect(close).toHaveBeenCalledWith(true);
  });

  it('should use the danger variant for destructive confirmation', () => {
    const buttonElements = fixture.debugElement.queryAll(
      By.directive(Button),
    );

    const confirmButton = buttonElements[1]?.componentInstance as Button;

    expect(confirmButton.variant()).toBe('danger');
  });

  it('should use the secondary variant for cancellation', () => {
    const buttonElements = fixture.debugElement.queryAll(
      By.directive(Button),
    );

    const cancelButton = buttonElements[0]?.componentInstance as Button;

    expect(cancelButton.variant()).toBe('secondary');
  });
});
