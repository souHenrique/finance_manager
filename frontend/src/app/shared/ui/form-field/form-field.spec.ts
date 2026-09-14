import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormField } from './form-field';

describe('FormField', () => {
  let fixture: ComponentFixture<FormField>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormField],
    }).compileComponents();

    fixture = TestBed.createComponent(FormField);
    fixture.componentRef.setInput('label', 'Descrição');
    fixture.componentRef.setInput('controlId', 'description');
    fixture.detectChanges();
  });

  it('should connect the label to the configured control id', () => {
    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;

    expect(label.htmlFor).toBe('description');
    expect(label.textContent).toContain('Descrição');
  });

  it('should render hint text with an accessible id', () => {
    fixture.componentRef.setInput('hint', 'Informe uma descrição');
    fixture.detectChanges();

    const hint = fixture.nativeElement.querySelector('.form-field__hint') as HTMLElement;

    expect(hint.id).toBe('description-hint');
    expect(hint.textContent).toContain('Informe uma descrição');
  });

  it('should render error instead of hint', () => {
    fixture.componentRef.setInput('hint', 'Informe uma descrição');
    fixture.componentRef.setInput('error', 'A descrição é obrigatória');
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.form-field__error') as HTMLElement;

    expect(error.id).toBe('description-error');
    expect(error.textContent).toContain('A descrição é obrigatória');

    expect(fixture.nativeElement.querySelector('.form-field__hint')).toBeNull();
  });

  it('should indicate a required field with text and symbol', () => {
    fixture.componentRef.setInput('required', true);
    fixture.detectChanges();

    const label = fixture.nativeElement.querySelector('label') as HTMLLabelElement;

    expect(label.textContent).toContain('*');
    expect(label.textContent).toContain('obrigatório');
  });
});
