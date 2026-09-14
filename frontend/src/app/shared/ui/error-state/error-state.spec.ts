import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorState } from './error-state';

describe('ErrorState', () => {
  let fixture: ComponentFixture<ErrorState>;
  let component: ErrorState;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorState],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorState);
    component = fixture.componentInstance;
    fixture.componentRef.setInput(
      'message',
      'Verifique sua conexão e tente novamente.',
    );
    fixture.detectChanges();
  });

  it('should render the default title and required message', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h2')?.textContent).toContain(
      'Não foi possível carregar os dados',
    );

    expect(element.querySelector('p')?.textContent).toContain(
      'Verifique sua conexão e tente novamente.',
    );
  });

  it('should use alert role', () => {
    const errorState = fixture.nativeElement.querySelector(
      '.error-state',
    ) as HTMLElement;

    expect(errorState.getAttribute('role')).toBe('alert');
  });

  it('should render retry by default', () => {
    const button = fixture.nativeElement.querySelector(
      'button',
    ) as HTMLButtonElement;

    expect(button).not.toBeNull();
    expect(button.textContent).toContain('Tentar novamente');
  });

  it('should emit retry when the button is clicked', () => {
    const retry = vi.fn();

    component.retry.subscribe(retry);

    const button = fixture.nativeElement.querySelector(
      'button',
    ) as HTMLButtonElement;

    button.click();

    expect(retry).toHaveBeenCalledOnce();
  });

  it('should hide retry when retryable is false', () => {
    fixture.componentRef.setInput('retryable', false);
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('button'),
    ).toBeNull();
  });

  it('should use a custom title', () => {
    fixture.componentRef.setInput(
      'title',
      'Erro ao carregar orçamentos',
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h2')?.textContent).toContain(
      'Erro ao carregar orçamentos',
    );
  });
});
